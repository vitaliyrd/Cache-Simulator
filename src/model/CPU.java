package model;

import java.util.Map;

/**
 * Created by Vitaliy on 6/3/15.
 */
public class CPU {
    private Cache l1d;
    private Cache l1i;
    private Cache l2;
    private System system;

    private int executes = 0;

    public CPU(Map<String, Integer> configuration, System system) {
        // Configure l1d
        l1d = new Cache(configuration.get("l1d_blocks"), configuration.get("l1d_blockSize"),
                configuration.get("l1d_associativity"), configuration.get("l1d_latency"));

        // Configure l1i
        l1i = new Cache(configuration.get("l1i_blocks"), configuration.get("l1i_blockSize"),
                configuration.get("l1i_associativity"), configuration.get("l1i_latency"));

        // Configure l2
        l2 = new Cache(configuration.get("l2_blocks"), configuration.get("l2_blockSize"),
                configuration.get("l2_associativity"), configuration.get("l2_latency"));

        // Hold the instance of System for callbacks.
        this.system = system;
    }

    public void execute(Instruction instruction) {
        // Fetch the instruction.
        readInstructionMemory(instruction.instruction);

        if(instruction.memoryAction == Instruction.MemoryAction.READ) {
            readDataMemory(instruction.data);
        } else if(instruction.memoryAction == Instruction.MemoryAction.WRITE) {
            writeMemory(instruction.data);
        }

        executes++;
    }

    private void readInstructionMemory(int address) {
        // First try l1i
        boolean miss = !l1i.locate(address);

        // Next try l2
        if(miss) {
            miss = l2.locate(address);
        }

        // Finally, ask System to fetch the instruction
        // Fetch the address into the l1i and l2 caches (as both were misses).
        if(miss) {
            system.fetch(address);
            l2.fetch(address);
            l1i.fetch(address);
        }
        // Otherwise, fetch the address into the l1i cache (as it was the only miss).
        else {
            l1i.fetch(address);
        }
    }

    private void readDataMemory(int address) {
        // First try l1d
        boolean miss = !l1d.locate(address);

        // Next try l2
        if(miss) {
            miss = l2.locate(address);
        }

        // Finally, ask System to fetch the instruction
        // Fetch the address into the l1d and l2 caches (as both were misses).
        if(miss) {
            system.fetch(address);
            l2.fetch(address);
            l1d.fetch(address);
        }
        // Otherwise, fetch the address into the l1d cache (as it was the only miss).
        else {
            l1d.fetch(address);
        }
    }

    private void writeMemory(int address) {

    }
}