package model;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * SystemBus class that holds all of the CPUs and memories, and responds to requests made by the system components.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class SystemBus {
    public enum WriteScheme {WRITEBACK, WRITETHROUGH}
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
    private WriteScheme write;

    private int runningTime = 0;

    public SystemBus(Map<String, Integer> config) {
        cpu1 = new CPU(config, this);
        cpu2 = new CPU(config, this);
        l3 = new Cache(config.get("l3_blocks"), config.get("l3_blockSize"), config.get("l3_associativity"),
                config.get("l3_latency"), this);
        lm1 = new Memory(config.get("lm1_size"), config.get("lm1_readLatency"), config.get("lm1_writeLatency"));
        lm2 = new Memory(config.get("lm2_size"), config.get("lm2_readLatency"), config.get("lm2_writeLatency"));

        if(config.get("writeScheme") == 0) {
            write = WriteScheme.WRITEBACK;
        } else {
            write = WriteScheme.WRITETHROUGH;
        }
    }

    public void execute(Instruction instruction, int cpu) {
        if(cpu == 1) {
            runningTime += cpu1.execute(instruction);
        } else if(cpu == 2) {
            runningTime += cpu2.execute(instruction);
        }
    }

    public Map<String, Integer> gatherStatistics() {
        Map<String, Integer> stats = new HashMap<>();

        stats.put("CPU Count", 2);
        stats.put("Running Time", runningTime);
        // CPU 1
        stats.put("CPU #1 L1i Misses", cpu1.getL1i().getMisses());
        stats.put("CPU #1 L1i Hits", cpu1.getL1i().getHits());
        stats.put("CPU #1 L1i Accesses", cpu1.getL1i().getAccesses());
        stats.put("CPU #1 L1d Misses", cpu1.getL1d().getMisses());
        stats.put("CPU #1 L1d Hits", cpu1.getL1d().getHits());
        stats.put("CPU #1 L1d Accesses", cpu1.getL1d().getAccesses());
        stats.put("CPU #1 L2 Misses", cpu1.getL2().getMisses());
        stats.put("CPU #1 L2 Hits", cpu1.getL2().getHits());
        stats.put("CPU #1 L2 Accesses", cpu1.getL2().getAccesses());
        stats.put("CPU #1 Instruction Count", cpu1.getInstructionCount());

        // CPU 2
        stats.put("CPU #2 L1i Misses", cpu2.getL1i().getMisses());
        stats.put("CPU #2 L1i Hits", cpu2.getL1i().getHits());
        stats.put("CPU #2 L1i Accesses", cpu2.getL1i().getAccesses());
        stats.put("CPU #2 L1d Misses", cpu2.getL1d().getMisses());
        stats.put("CPU #2 L1d Hits", cpu2.getL1d().getHits());
        stats.put("CPU #2 L1d Accesses", cpu2.getL1d().getAccesses());
        stats.put("CPU #2 L2 Misses", cpu2.getL2().getMisses());
        stats.put("CPU #2 L2 Hits", cpu2.getL2().getHits());
        stats.put("CPU #2 L2 Accesses", cpu2.getL2().getAccesses());
        stats.put("CPU #2 Instruction Count", cpu2.getInstructionCount());

        // L3
        stats.put("L3 Misses", l3.getMisses());
        stats.put("L3 Hits", l3.getHits());
        stats.put("L3 Accesses", l3.getAccesses());

        // LM1
        stats.put("LM1 Misses", lm1.getReads());
        stats.put("LM1 Hits", lm1.getWrites());

        // LM2
        stats.put("LM2 Misses", lm2.getReads());
        stats.put("LM2 Hits", lm2.getWrites());

        return stats;
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
                memWrite(address);
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
            int newIndex1 = caller.getL1d().add(address);
            int newIndex2 = caller.getL2().add(address);
            if(otherCPU.getL2().isExclusive(index)) {
                otherCPU.getL2().markShared(otherCPU.getL2().locate(address));
                caller.getL1d().markShared(newIndex1);
                caller.getL2().markShared(newIndex2);
            } else if(otherCPU.getL2().isModified(index)) {
                time += memWrite(address);
                otherCPU.getL2().markShared(otherCPU.getL2().locate(address));
                otherCPU.getL2().markNotModified(otherCPU.getL2().locate(address));
                caller.getL1d().markShared(newIndex1);
                caller.getL1d().markNotModified(newIndex1);
                caller.getL2().markShared(newIndex2);
                caller.getL2().markNotModified(newIndex2);
            }
            return time;
        }

        // Next check the l3
        time += l3.getLatecy();
        int index3 = l3.locate(address);
        if(index3 != -1) {
            int newIndex1 = caller.getL1d().add(address);
            int newIndex2 = caller.getL2().add(address);
            if(l3.isModified(index3)) {
                time += memWrite(address);
                l3.markNotModified(otherCPU.getL2().locate(address));
                caller.getL1d().markNotModified(newIndex1);
                caller.getL2().markNotModified(newIndex2);
            }
            return time;
        }

        // Next look in the lm1
        time += lm1.getReadLatency();
        if(lm1.read(address)) {
            index = caller.getL1d().add(address);
            caller.getL1d().markExclusive(index);
            index = caller.getL2().add(address);
            caller.getL2().markExclusive(index);
            index = l3.add(address);
            l3.markExclusive(index);
            return time;
        }

        // Finally look in the lm2 (assumed to always be a hit)
        time += lm2.getReadLatency();
        lm2.read(address);
        index = caller.getL1d().add(address);
        caller.getL1d().markExclusive(index);
        index = caller.getL2().add(address);
        caller.getL2().markExclusive(index);
        index = l3.add(address);
        l3.markExclusive(index);
        return time;
    }

    public int issueWriteRequest(int address, CPU caller) {
        int time = 0;

        CPU otherCPU;
        if(caller == cpu1) {
            otherCPU = cpu2;
        } else if(caller == cpu2) {
            otherCPU = cpu1;
        } else {
            lm1.write(address);
            time += lm1.getWriteLatency();
            lm2.write(address);
            time += lm2.getWriteLatency();
            return time;
        }

        int index1 = otherCPU.getL1d().locate(address);
        int index2 = otherCPU.getL2().locate(address);
        time += otherCPU.getL1d().getLatecy();
        if(index1 != -1) {
            if(otherCPU.getL1d().isModified(index1)) {
                memWrite(address);
                otherCPU.getL1d().markInvalid(index1);
                otherCPU.getL2().markInvalid(index2);
            } else {
                otherCPU.getL1d().markInvalid(index1);
                otherCPU.getL2().markInvalid(index2);
            }
        }
        time += otherCPU.getL2().getLatecy();
        if(index2 != -1) {
            if(otherCPU.getL2().isModified(index2)) {
                memWrite(address);
                otherCPU.getL2().markInvalid(index2);
            } else {
                otherCPU.getL2().markInvalid(index2);
            }
        }

        return time;
    }

    /**
     * Invalidates block in the caches of the other CPUs that contain the passed address.
     * @param address
     * @param caller
     * @return
     */
    public int issueRequestForOwnership(int address, CPU caller) {
        int time = 0;

        CPU otherCPU;
        if(caller == cpu1) {
            otherCPU = cpu2;
        } else {
            otherCPU = cpu1;
        }

        int index1i = otherCPU.getL1i().locate(address);
        int index1d = otherCPU.getL1d().locate(address);
        int index2 = otherCPU.getL2().locate(address);

        if(index1i != -1) {
            time += otherCPU.getL1i().getLatecy();
            otherCPU.getL1i().markInvalid(index1i);
        }
        if(index1d != -1) {
            time += otherCPU.getL1d().getLatecy();
            otherCPU.getL1d().markInvalid(index1d);
        }
        if(index2 != -1) {
            time += otherCPU.getL2().getLatecy();
            otherCPU.getL2().markInvalid(index2);
        }

        return time;
    }

    private int memWrite(int address) {
        int time = 0;

        if(write == WriteScheme.WRITETHROUGH) {
            lm1.write(address);
            time += lm1.getWriteLatency();
            lm2.write(address);
            time += lm2.getWriteLatency();
        }

        // Otherwise, the caches have already been updated.

        return time;
    }
}