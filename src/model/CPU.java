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

        // First try the L1 instruction cache.
        if(debug) debuggingOutput.print("L1i: ");
        int indexL1 = l1i.locate(address);
        if(indexL1 != -1){
            time += l1i.getLatency();
            return time;
        }
        
        // Next try the L2 cache.
        if(debug) debuggingOutput.print("L2: ");
        int indexL2 = l2.locate(address);
        if(indexL2 != -1) {
            // Copy the value to L1 and set it to the same MESI state as L2.
            if(debug) debuggingOutput.print("L1i: ");
            int newIndexL1 = l1i.add(address);
            l1i.setState(newIndexL1, l2.getState(indexL2));

            time += l2.getLatency();
            return time;
        }

        // Finally, request a read on the system bus.
        if(debug) debuggingOutput.println("System read request placed");
        time += system.issueReadRequest(address, this, true);
        return time;
    }

    private int readData(long address) {
        int time = 0;

        // First try the L1 data cache.
        if(debug) debuggingOutput.print("L1d: ");
        int indexL1 = l1d.locate(address);
        if(indexL1 != -1){
            time += l1d.getLatency();
            return time;
        }

        // Next try the L2 cache.
        if(debug) debuggingOutput.print("L2: ");
        int indexL2 = l2.locate(address);
        if(indexL2 != -1) {
            // Copy the value to L1 and set it to the same MESI state as L2.
            if(debug) debuggingOutput.print("L1d: ");
            int newIndexL1 = l1d.add(address);
            l1d.setState(newIndexL1, l2.getState(indexL2));

            time += l2.getLatency();
            return time;
        }

        // Finally, request a read on the system bus.
        if(debug) debuggingOutput.println("System read request placed");
        time += system.issueReadRequest(address, this, false);
        return time;
    }

    private int writeData(long address) {
        int time = 0;

        // If the previous data exists already in the cache:
        if(debug) debuggingOutput.print("L1d: ");
        int indexL1 = l1d.locate(address);
        time += l1d.getLatency();
        if(debug) debuggingOutput.print("L1d: ");
        int indexL2 = l2.locate(address);
        time += l2.getLatency();

        if(indexL1 != -1) {
            time += l1d.getLatency();       // Increment time a second time, because a write is being performed.

            if(l1d.isModified(indexL1)) {
                // Update the value (no state change).
            } else if(l1d.isExclusive(indexL1)) {
                // Update the value.
                l1d.setState(indexL1, CacheLine.MESI.Modified);
                l2.setState(indexL2, CacheLine.MESI.Modified);
                system.incrementModified(CacheLine.MESI.Exclusive); // MESI change: Exclusive -> Modified
            } else if(l1d.isShared(indexL1)) {
                time += system.issueRequestForOwnership(address, this);
                // Update the value.
                l1d.setState(indexL1, CacheLine.MESI.Modified);
                l2.setState(indexL2, CacheLine.MESI.Modified);
                system.incrementModified(CacheLine.MESI.Shared);    // MESI change: Shared -> Modified
            }
        } else if(indexL2 != -1) {
            time += l2.getLatency();    // Increment time a second time, because a write is being performed.

            if(l2.isModified(indexL2)) {
                // Update the value (no state change).
            } else if(l2.isExclusive(indexL2)) {
                // Update the value.
                l2.setState(indexL2, CacheLine.MESI.Modified);
                system.incrementModified(CacheLine.MESI.Exclusive); // MESI change: Exclusive -> Modified
            } else if(l2.isShared(indexL2)) {
                time += system.issueRequestForOwnership(address, this);
                // Update the value.
                l2.setState(indexL2, CacheLine.MESI.Modified);
                system.incrementModified(CacheLine.MESI.Shared);    // MESI change: Shared -> Modified
            }

            // Bring the cache line in to l1d.
            int newIndexL1 = l1d.add(address);
            time += l1d.getLatency();   // Increment time a second time, because a write is being performed.
            l1d.setState(newIndexL1, CacheLine.MESI.Modified);
        } else {
            time += system.issueWriteRequest(address, this);
        }

        return time;
    }
}
