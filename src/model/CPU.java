package model;

import java.util.Map;

/**
 * CPU class that can execute instructions, as well as request memory accesses from the MainSystem.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class CPU {
    public boolean debug = false;

    private Cache l1d;
    private Cache l1i;
    private Cache l2;
    private MainSystem mainSystem;

    private int instructions = 0;

    public CPU(Map<String, Integer> configuration, MainSystem mainSystem) {
        // Configure l1d
        l1d = new Cache(configuration.get("l1d_blocks"), configuration.get("l1d_blockSize"),
                configuration.get("l1d_associativity"), configuration.get("l1d_latency"));

        // Configure l1i
        l1i = new Cache(configuration.get("l1i_blocks"), configuration.get("l1i_blockSize"),
                configuration.get("l1i_associativity"), configuration.get("l1i_latency"));

        // Configure l2
        l2 = new Cache(configuration.get("l2_blocks"), configuration.get("l2_blockSize"),
                configuration.get("l2_associativity"), configuration.get("l2_latency"));

        // Hold the instance of MainSystem for callbacks.
        this.mainSystem = mainSystem;
    }

    public void execute(Instruction instruction) {
        l1d.debug = debug;
        l1i.debug = debug;
        l2.debug = debug;

        // Fetch the instruction.
        if(debug) System.out.println("Fetching instruction:");
        readInstructionMemory(instruction.instruction);

        if(instruction.memoryAction == Instruction.MemoryAction.READ) {
            if(debug) System.out.println("Memory read:");
            readDataMemory(instruction.data);
        } else if(instruction.memoryAction == Instruction.MemoryAction.WRITE) {
            if(debug) System.out.println("Memory write:");
            writeMemory(instruction.data);
        }

        instructions++;
    }

    /**
     * Returns the total time the CPU had to wait on its two levels of Caches.
     *
     * @return Total Cache wait time in nanoseconds.
     */
    public int getCacheWaitTime() {
        return l1i.getTotalTime() + l1d.getTotalTime() + l2.getTotalTime();
    }

    private void readInstructionMemory(int address) {
        // First try l1i
        if(debug) System.out.print("l1i: ");
        boolean hit = l1i.locate(address);
        if(hit) return;

        // Next try l2
        if(debug) System.out.print("l2: ");
        hit = l2.locate(address);
        if(hit) {
            l1i.fetch(address);
            return;
        }

        // Finally, ask MainSystem to fetch the instruction
        // Fetch the address into the l1i and l2 caches (as both were misses).
        mainSystem.fetch(address);
        if(debug) System.out.print("l2: ");
        l2.fetch(address);
        if(debug) System.out.print("l1i: ");
        l1i.fetch(address);
    }

    private void readDataMemory(int address) {
        // First try l1d
        if(debug) System.out.print("l1d: ");
        boolean hit = l1d.locate(address);
        if(hit) return;

        // Next try l2
        if(debug) System.out.print("l2: ");
        hit = l2.locate(address);
        if(hit) {
            l1d.fetch(address);
            return;
        }

        // Finally, ask MainSystem to fetch the instruction
        // Fetch the address into the l1d and l2 caches (as both were misses).
        mainSystem.fetch(address);
        if(debug) System.out.print("l2: ");
        l2.fetch(address);
        if(debug) System.out.print("l1d: ");
        l1d.fetch(address);
    }

    private void writeMemory(int address) {
        if(debug) System.out.print("l2: ");
        l2.write(address);
        if(debug) System.out.print("l1d: ");
        l1d.write(address);
        mainSystem.write(address);
    }
}