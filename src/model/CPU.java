package model;

import java.io.PrintStream;
import java.util.Map;

/**
 * CPU class that can execute instructions, as well as request memory accesses from the MainSystem.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class CPU {
    /**
     * If enabled, prints debugging messages to the console.
     */
    public boolean debug = false;
    /**
     * Where to print debugging messages - System.out by default.
     */
    public PrintStream debuggingOutput = System.out;
    
    /**
     * Cache L1I.
     */
    private Cache l1i;
    
    /**
     * Cache L1D.
     */
    private Cache l1d;
    
    /**
     * Shared Cache
     */
    private Cache l2;

    private SystemBus system;

    private int instructionCount = 0;

    public CPU(Map<String, Integer> config, SystemBus system) {
        // Configure l1d
        l1d = new Cache(config.get("l1_blocks"), config.get("block_size"),
                config.get("associativity"), config.get("l1_latency"), system);

        // Configure l1i
        l1i = new Cache(config.get("l1_blocks"), config.get("block_size"),
                config.get("associativity"), config.get("l1_latency"), system);

        // Configure l2
        l2 = new Cache(config.get("l2_blocks"), config.get("block_size"),
                config.get("associativity"), config.get("l2_latency"), system);

        this.system = system;   // Todo: This creates an interdependency; fix if time permits.
                                // Message-passing is a better way to do this.
    }

    public int getInstructionCount() {
        return instructionCount;
    }
    /**
     * Executes the passed Instruction.
     *
     * @param instruction The Instruction to execute.
     * @return Execution time in milliseconds.
     */
    public int execute(Instruction instruction) {
        int time = 0;

        // Fetch the instruction.
        if(debug) debuggingOutput.println("Fetching instruction:");
        time += readInstruction(instruction.instruction);

        // Execute the memory operation.
        if(instruction.memoryAction == Instruction.MemoryAction.READ) {
            if(debug) debuggingOutput.println("Memory read:");
            time += readData(instruction.data);
        } else if(instruction.memoryAction == Instruction.MemoryAction.WRITE) {
            if(debug) debuggingOutput.println("Memory write:");
            time += writeData(instruction.data);
        }

        instructionCount++;
        return time;
    }

    public Cache getL1i() {
        return l1i;
    }

    public Cache getL1d() {
        return l1d;
    }

    public Cache getL2() {
        return l2;
    }

    private int readInstruction(long address) {
        int time = 0;

        // First try l1i
        if(debug) debuggingOutput.print("l1i: ");
        int index1 = l1i.locate(address);
        if(index1 != -1){
            time += l1i.getLatecy();
            return time;
        }
        
        // Next try l2
        if(debug) debuggingOutput.print("l2: ");
        int index2 = l2.locate(address);
        if(index2 != -1) {
            if(debug) debuggingOutput.print("l1i: ");
            index1 = l1i.add(address);
            l1i.markValid(index1);

            time += l2.getLatecy();
            return time;
        }

        // If all caches in CPU missed, request a read on the system bus.
        if(debug) debuggingOutput.print("System read request placed");
        time += system.issueReadRequest(address, this); //very clever
        return time;
    }

    private int readData(long address) {
        int time = 0;

        // First try l1d
        if(debug) debuggingOutput.print("l1d: ");
        int index1 = l1d.locate(address);
        if(index1 != -1){
            time += l1d.getLatecy();
            return time;
        }

        // Next try l2
        if(debug) debuggingOutput.print("l2: ");
        int index2 = l2.locate(address);
        if(index2 != -1) {
            if(debug) debuggingOutput.print("l1d: ");
            index1 = l1d.add(address);
            l1d.markValid(index1);

            time += l2.getLatecy();
            return time;
        }

        // If all caches in CPU missed, request a read on the system bus.
        if(debug) debuggingOutput.print("System read request placed");
        time += system.issueReadRequest(address, this);
        return time;
    }

    private int writeData(long address) {
        int time = 0;

        // If the previous data exists already in the cache:
        if(debug) debuggingOutput.print("l1d: ");
        int index1 = l1d.locate(address);   //Saving the index of the address as index1
        time += l1d.getLatecy();            //Getting latency
        int index2 = l2.locate(address);    //Saving the index of the shared address as index2
        time += l2.getLatecy();             //Getting latency

        if(index1 != -1) {                  //If valid index
            time += l1d.getLatecy();        //Increment time again, since we first had to look for the data,
                                            // and then write to the cache.

            if(l1d.isModified(index1)) {
                // Update the value (no state change).
            } else if(l1d.isExclusive(index1)) {
                // Update the value.
                l1d.markModified(index1);
                system.incrementModified(CacheLine.MESI.Exclusive); // MESI change: Exclusive -> Modified
            } else if(l1d.isShared(index1)) {
                time += system.issueRequestForOwnership(address, this);
                // Update the value.
                l1d.markModified(index1);
                system.incrementModified(CacheLine.MESI.Shared);    // MESI change: Shared -> Modified
                l1d.markExclusive(index1);
            }

            // Update state in l2.
            l2.markExclusive(index2);
            l2.markModified(index2);
        } else if(index2 != -1) {           //Address not found in L1, so we look in L2
            time += l2.getLatecy();

            if(l2.isModified(index2)) {
                // Update the value (no state change).
            } else if(l2.isExclusive(index2)) {
                // Update the value.
                l2.markModified(index2);
                system.incrementModified(CacheLine.MESI.Exclusive); // MESI change: Exclusive -> Modified
            } else if(l2.isShared(index2)) {
                time += system.issueRequestForOwnership(address, this);
                // Update the value.
                l2.markModified(index2);
                system.incrementModified(CacheLine.MESI.Shared);    // MESI change: Shared -> Modified
                l2.markExclusive(index2);
            }

            // Bring the cache line in to l1d.
            l1d.add(address); //Write back
            l1d.markExclusive(index1);
            l1d.markModified(index1);
        } else {
            time += system.issueWriteRequest(address, this);
            index1 = l1d.add(address);   // Write to l1d
            index2 = l2.add(address);    // Write to l2
            l1d.markModified(index1);
            l2.markModified(index2);
        }

        return time;
    }
}
