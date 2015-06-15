package model;

import java.io.PrintStream;
import java.util.Map;

/**
 * SystemBus class that holds all of the CPUs and memories, and responds to requests made by the system components.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class SystemBus {
    /**
     * If enabled, prints debugging messages to the console.
     */
    public boolean debug = false;
    /**
     * Where to print debugging messages - System.out by default.
     */
    public PrintStream debuggingOutput = System.out;

    private CPU cpu1;
    private CPU cpu2;
    private Cache l3;
    private Memory lm1;
    private Memory lm2;

    public SystemBus(Map<String, Integer> config) {
        cpu1 = new CPU(config, this);
        cpu2 = new CPU(config, this);
        l3 = new Cache(config.get("l3_blocks"), config.get("l3_blockSize"), config.get("l3_associativity"),
                config.get("l3_latency"));
        lm1 = new Memory(config.get("lm1_size"), config.get("lm1_readLatency"), config.get("lm1_writeLatency"));
        lm2 = new Memory(config.get("lm2_size"), config.get("lm2_readLatency"), config.get("lm2_writeLatency"));
    }

    /**
     * Response to a CPU's request for data.
     * Checks the cache contents of the other CPUs and the memories in the system.
     * @param address
     * @param caller
     * @return
     */
    public int issueReadRequest(int address, CPU caller) {
        int time = 0;

        CPU otherCPU;
        if(caller == cpu1) {
            otherCPU = cpu2;
        } else {
            otherCPU = cpu1;
        }

        // First check for any copies that exist in the other CPUs' caches.
        int index = otherCPU.getL1d().locate(address);
        time += otherCPU.getL1d().getLatecy();
        if(index != -1) {
            int newIndex1 = caller.getL1d().add(address);
            int newIndex2 = caller.getL2().add(address);
            if(otherCPU.getL1d().isExclusive(index)) {
                // MESI Change: Exclusive -> Shared
                otherCPU.getL1d().markShared(index);
                otherCPU.getL2().markShared(otherCPU.getL2().locate(address));
                caller.getL1d().markShared(newIndex1);
                caller.getL2().markShared(newIndex2);
            } else if(otherCPU.getL1d().isModified(index)) {
                // TODO: Write back to memory
                // MESI Change: Modified -> Shared
                otherCPU.getL1d().markShared(index);
                otherCPU.getL1d().markNotModified(index);
                otherCPU.getL2().markShared(otherCPU.getL2().locate(address));
                otherCPU.getL2().markNotModified(otherCPU.getL2().locate(address));
                caller.getL1d().markShared(newIndex1);
                caller.getL1d().markNotModified(newIndex1);
                caller.getL2().markShared(newIndex2);
                caller.getL2().markNotModified(newIndex2);
            }
            return time;
        }
        index = otherCPU.getL2().locate(address);
        time += otherCPU.getL2().getLatecy();
        if(index != -1) {
            int newIndex2 = caller.getL2().add(address);
            if(otherCPU.getL2().isExclusive(index)) {
                otherCPU.getL2().markShared(otherCPU.getL2().locate(address));
                caller.getL2().markShared(newIndex2);
            } else if(otherCPU.getL2().isModified(index)) {
                // TODO: Write back to memory
                otherCPU.getL2().markShared(otherCPU.getL2().locate(address));
                otherCPU.getL2().markNotModified(otherCPU.getL2().locate(address));
                caller.getL2().markShared(newIndex2);
                caller.getL2().markNotModified(newIndex2);
            }
            return time;
        }

        // Next look in the lm1
        time += lm1.getReadLatency();
        if(lm1.read(address)) {
            index = caller.getL1d().add(address);
            caller.getL1d().markExclusive(index);
            return time;
        }

        // Finally look in the lm2 (assumed to always be a hit)
        time += lm2.getReadLatency();
        lm2.read(address);
        index = caller.getL1d().add(address);
        caller.getL1d().markExclusive(index);
        return time;
    }

    public int issueWriteRequest(int address, CPU caller) {
        CPU otherCPU;
        if(caller == cpu1) {
            otherCPU = cpu2;
        } else {
            otherCPU = cpu1;
        }

        return 0;
    }

    public void issueRequestForOwnership(int address, CPU caller) {
        CPU otherCPU;
        if(caller == cpu1) {
            otherCPU = cpu2;
        } else {
            otherCPU = cpu1;
        }


    }
}
