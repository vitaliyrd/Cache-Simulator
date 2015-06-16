package model;

import java.io.PrintStream;
import java.util.LinkedHashMap;
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

    /**
     * Row: The starting state
     * Column: The ending state
     * Order: Modified, Exclusive, Shared, Invalid
     */
    private int stateChanges[][];
    private int runningTime = 0;

    public SystemBus(Map<String, Integer> config) {
        cpu1 = new CPU(config, this);
        cpu2 = new CPU(config, this);
        l3 = new Cache(config.get("l3_blocks"), config.get("block_size"), config.get("associativity"),
                config.get("l3_latency"), this);
        lm1 = new Memory(config.get("lm1_size"), config.get("lm1_readLatency"), config.get("lm1_writeLatency"));
        lm2 = new Memory(config.get("lm2_size"), config.get("lm2_readLatency"), config.get("lm2_writeLatency"));

        if(config.get("writeScheme") == 0) {
            write = WriteScheme.WRITEBACK;
        } else {
            write = WriteScheme.WRITETHROUGH;
        }

        stateChanges = new int[4][4];
    }

    public void execute(Instruction instruction, int cpu) {
        if(cpu == 1) {
            runningTime += cpu1.execute(instruction);
        } else if(cpu == 2) {
            runningTime += cpu2.execute(instruction);
        }
    }

    public Map<String, Integer> gatherStatistics() {
        Map<String, Integer> stats = new LinkedHashMap<>();

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
        stats.put("LM1 Reads", lm1.getReads());
        stats.put("LM1 Writes", lm1.getWrites());

        // LM2
        stats.put("LM2 Reads", lm2.getReads());
        stats.put("LM2 Writes", lm2.getWrites());

        return stats;
    }

    public void incrementModified(CacheLine.MESI previousState) {
        if(previousState == CacheLine.MESI.Exclusive) stateChanges[1][0]++;
        else if(previousState == CacheLine.MESI.Shared) stateChanges[2][0]++;
        else if(previousState == CacheLine.MESI.Invalid) stateChanges[3][0]++;
    }

    public int[][] getStateChanges() {
        return stateChanges;
    }

    public int saveModifiedCacheLine(long address) {
        return memWrite(address);
    }

    /**
     * Response to a CPU's request for data.
     * Checks the cache contents of the other CPUs and the memories in the system.
     * @param address
     * @param caller
     * @param instruction
     * @return
     */
    public int issueReadRequest(long address, CPU caller, boolean instruction) {
        int time = 0;

        CPU otherCPU;
        Cache callerL1;
        Cache otherL1;
        if(caller == cpu1) otherCPU = cpu2;
        else otherCPU = cpu1;

        // If the read request is for an instruction, we need to use the L1i cache instead of the L2d cache.
        if(instruction) {
            callerL1 = caller.getL1i();
            otherL1 = otherCPU.getL1i();
        } else {
            callerL1 = caller.getL1d();
            otherL1 = otherCPU.getL1d();
        }

        // First check the L1 caches of the other CPU.
        if(debug) debuggingOutput.println("Other CPU L1: ");
        int indexL1 = otherL1.locate(address);
        time += otherL1.getLatency();
        if(indexL1 != -1) {
            // Copy the data to the calling CPU's L1 and L2 caches.
            if(debug) debuggingOutput.println("Calling CPU L1: ");
            int newIndex1 = callerL1.add(address);
            if(debug) debuggingOutput.println("Calling CPU L2: ");
            int newIndex2 = caller.getL2().add(address);

            // Set all copies of the data into the MESI shared state.
            // If the other CPU's data was modified, we need to write it to memory before sharing it.
            if(otherL1.isModified(indexL1)) {
                time += memWrite(address);

                stateChanges[0][2]++;   // MESI Change: Modified -> Shared

                // Update other CPU's caches.
                otherL1.setState(indexL1, CacheLine.MESI.Shared);
                if(debug) debuggingOutput.println("Other CPU L2: ");
                int indexL2 = otherCPU.getL2().locate(address);
                otherCPU.getL2().setState(indexL2, CacheLine.MESI.Shared);

                // Update calling CPU's caches.
                callerL1.setState(newIndex1, CacheLine.MESI.Shared);
                caller.getL2().setState(newIndex2, CacheLine.MESI.Shared);

                // Update the L3 cache.
                if(debug) debuggingOutput.println("L3: ");
                int l3Index = l3.locate(address);
                l3.setState(l3Index, CacheLine.MESI.Shared);
            }
            // If the other CPU's data was exclusive, it is now shared since it's being used in another processor.
            else if(otherL1.isExclusive(indexL1)) {
                stateChanges[1][2]++;   // MESI Change: Exclusive -> Shared

                // Update other CPU's caches.
                otherL1.setState(indexL1, CacheLine.MESI.Shared);
                if(debug) debuggingOutput.println("Other CPU L2: ");
                int indexL2 = otherCPU.getL2().add(address);
                otherCPU.getL2().setState(indexL2, CacheLine.MESI.Shared);

                // Update calling CPU's caches.
                callerL1.setState(newIndex1, CacheLine.MESI.Shared);
                caller.getL2().setState(newIndex2, CacheLine.MESI.Shared);

                // Update the L3 Cache.
                if(debug) debuggingOutput.println("L3: ");
                int l3Index = l3.add(address);
                l3.setState(l3Index, CacheLine.MESI.Shared);
            }

            return time;
        }

        // Next check the L2 cache of the other CPU.
        int index2 = otherCPU.getL2().locate(address);
        time += otherCPU.getL2().getLatency();
        if(index2 != -1) {
            // Copy the data to the calling CPU's L1 and L2 caches.
            if(debug) debuggingOutput.println("Calling CPU L1: ");
            int newIndex1 = callerL1.add(address);
            if(debug) debuggingOutput.println("Calling CPU L2: ");
            int newIndex2 = caller.getL2().add(address);

            // Set all copies of the data into the MESI shared state.
            // If the other CPU's data was modified, we need to write it to memory before sharing it.
            if(otherCPU.getL2().isModified(index2)) {
                time += memWrite(address);

                stateChanges[0][2]++;   // MESI Change: Modified -> Shared

                // Update other CPU's cache.
                otherCPU.getL2().setState(index2, CacheLine.MESI.Shared);

                // Update calling CPU's caches.
                callerL1.setState(newIndex1, CacheLine.MESI.Shared);
                caller.getL2().setState(newIndex2, CacheLine.MESI.Shared);

                // Update the L3 cache.
                if(debug) debuggingOutput.println("L3: ");
                int l3Index = l3.locate(address);
                l3.setState(l3Index, CacheLine.MESI.Shared);
            }
            // If the other CPU's data was exclusive, it is now shared since it's being used in another processor.
            else if(otherCPU.getL2().isExclusive(index2)) {
                stateChanges[1][2]++;   // MESI Change: Exclusive -> Shared

                // Update other CPU's caches.
                otherCPU.getL2().setState(index2, CacheLine.MESI.Shared);

                // Update calling CPU's caches.
                callerL1.setState(newIndex1, CacheLine.MESI.Shared);
                caller.getL2().setState(newIndex2, CacheLine.MESI.Shared);

                // Update the L3 cache.
                if(debug) debuggingOutput.println("L3: ");
                int l3Index = l3.add(address);
                l3.setState(l3Index, CacheLine.MESI.Shared);
            }
            return time;
        }

        // Next check the L3 cache.
        time += l3.getLatency();
        if(debug) debuggingOutput.println("L3 Cache: ");
        int index3 = l3.locate(address);
        if(index3 != -1) {
            // Copy the data to the calling CPU's L1 and L2 caches.
            if(debug) debuggingOutput.println("Calling CPU L1: ");
            int newIndex1 = callerL1.add(address);
            if(debug) debuggingOutput.println("Calling CPU L2: ");
            int newIndex2 = caller.getL2().add(address);

            // If the data has been modified, we need to write it to memory before making it exclusive.
            // The data is exclusive because the calling CPU is the only one that has the data.
            if(l3.isModified(index3)) {
                time += memWrite(address);

                stateChanges[0][1]++;   // MESI Change: Modified -> Exclusive
            }

            // Update the cache states.
            l3.setState(index3, CacheLine.MESI.Exclusive);
            callerL1.setState(newIndex1, CacheLine.MESI.Exclusive);
            caller.getL2().setState(newIndex2, CacheLine.MESI.Exclusive);
            return time;
        }

        // Next check the LM1 (DRAM).
        time += lm1.getReadLatency();
        if (lm1.read(address)) {
            // Copy the data to the calling CPU's L1 and L2 caches.
            if(debug) debuggingOutput.println("Calling CPU L1: ");
            int newIndex1 = callerL1.add(address);
            if(debug) debuggingOutput.println("Calling CPU L2: ");
            int newIndex2 = caller.getL2().add(address);
            if(debug) debuggingOutput.println("L3: ");
            index3 = l3.add(address);

            stateChanges[3][1]++;   // MESI Change: Invalid -> Exclusive

            // Update the cache states.
            l3.setState(index3, CacheLine.MESI.Exclusive);
            callerL1.setState(newIndex1, CacheLine.MESI.Exclusive);
            caller.getL2().setState(newIndex2, CacheLine.MESI.Exclusive);
            return time;
        }

        // Finally, check the LM2 (PM).
        time += lm2.getReadLatency();
        if (lm2.read(address)) {
            // Copy the data to the calling CPU's L1 and L2 caches.
            if(debug) debuggingOutput.println("Calling CPU L1: ");
            int newIndex1 = callerL1.add(address);
            if(debug) debuggingOutput.println("Calling CPU L2: ");
            int newIndex2 = caller.getL2().add(address);
            if(debug) debuggingOutput.println("L3: ");
            index3 = l3.add(address);

            stateChanges[3][1]++;   // MESI Change: Invalid -> Exclusive

            // Update the cache states.
            l3.setState(index3, CacheLine.MESI.Exclusive);
            callerL1.setState(newIndex1, CacheLine.MESI.Exclusive);
            caller.getL2().setState(newIndex2, CacheLine.MESI.Exclusive);
        }
        return time;
    }

    public int issueWriteRequest(long address, CPU caller) {
        int time = 0;

        CPU otherCPU;
        if(caller == cpu1) {
            otherCPU = cpu2;
        } else {
            otherCPU = cpu1;
        }

        // First check the L1 cache of the other CPU for an occurrence of the address being written to.
        int index1 = otherCPU.getL1d().locate(address);
        int index2 = otherCPU.getL2().locate(address);
        time += otherCPU.getL1d().getLatency();
        time += otherCPU.getL2().getLatency();
        if(index1 != -1) {
            // If the address in the other CPU is modified, it must be saved to main memory before the calling CPU
            // makes it's write, to ensure memory consistency. Also, the address in the other CPU must be invalidated.
            if(otherCPU.getL1d().isModified(index1)) {
                time += memWrite(address);

                otherCPU.getL1d().setState(index1, CacheLine.MESI.Invalid);
                otherCPU.getL2().setState(index2, CacheLine.MESI.Invalid);

                stateChanges[0][3]++;   // MESI Change: Modified -> Invalid
            } else {
                otherCPU.getL1d().setState(index1, CacheLine.MESI.Invalid);
                if(index2 != -1) otherCPU.getL2().setState(index2, CacheLine.MESI.Invalid);

                if(otherCPU.getL1d().isExclusive(index1)) stateChanges[1][3]++;   // MESI Change: Exclusive -> Invalid
                else if(otherCPU.getL1d().isShared(index1)) stateChanges[2][3]++; // MESI Change: Shared -> Invalid
            }

            // Add the modified value to the calling CPU's cache.
            int newIndex1 = caller.getL1d().add(address);
            time+= caller.getL1d().getLatency();
            int newIndex2 = caller.getL2().add(address);
            time+= caller.getL2().getLatency();
            caller.getL1d().setState(newIndex1, CacheLine.MESI.Modified);
            caller.getL2().setState(newIndex2, CacheLine.MESI.Modified);

            // Also need to update the state in L3.
            int index3 = l3.add(address);
            time += l3.getLatency();
            l3.setState(index3, CacheLine.MESI.Modified);

            return time;
        }

        // Next check the L2 cache of the other CPU for an occurrence of the address being written to.
        if(index2 != -1) {
            // If the address in the other CPU is modified, it must be saved to main memory before the calling CPU
            // makes it's write, to ensure memory consistency. Also, the address in the other CPU must be invalidated.
            if(otherCPU.getL2().isModified(index2)) {
                time += memWrite(address);
                otherCPU.getL2().setState(index2, CacheLine.MESI.Invalid);
                stateChanges[0][3]++;   // MESI Change: Modified -> Invalid
            } else {
                otherCPU.getL2().setState(index2, CacheLine.MESI.Invalid);

                if(otherCPU.getL2().isExclusive(index2)) stateChanges[1][3]++;   // MESI Change: Exclusive -> Invalid
                else if(otherCPU.getL2().isShared(index2)) stateChanges[2][3]++; // MESI Change: Shared -> Invalid
            }

            // Add the modified value to the calling CPU's cache.
            int newIndex1 = caller.getL1d().add(address);
            time+= caller.getL1d().getLatency();
            int newIndex2 = caller.getL2().add(address);
            time+= caller.getL2().getLatency();
            caller.getL1d().setState(newIndex1, CacheLine.MESI.Modified);
            caller.getL2().setState(newIndex2, CacheLine.MESI.Modified);

            // Also need to update the state in L3.
            int index3 = l3.add(address);
            time += l3.getLatency();
            l3.setState(index3, CacheLine.MESI.Modified);

            return time;
        }

        // Next check the L3 cache for an occurrence of the address being written to.
        int index3 = l3.locate(address);
        time += l3.getLatency();
        if(index3 != -1) {
            // If the address in the L3 is modified, it must be saved to main memory before the calling CPU
            // makes it's write, to ensure memory consistency.
            if(l3.isModified(index3)) {
                time += memWrite(address);
            } else if(l3.isExclusive(index3))  {
                l3.setState(index3, CacheLine.MESI.Modified);
                stateChanges[1][3]++;   // MESI Change: Exclusive -> Invalid
            }

            // Add the modified value to the calling CPU's cache.
            int newIndex1 = caller.getL1d().add(address);
            time+= caller.getL1d().getLatency();
            int newIndex2 = caller.getL2().add(address);
            time+= caller.getL2().getLatency();
            caller.getL1d().setState(newIndex1, CacheLine.MESI.Modified);
            caller.getL2().setState(newIndex2, CacheLine.MESI.Modified);

            return time;
        }

        // Finally write to memory and update all caches.
        time += memWrite(address);
        index1 = caller.getL1d().add(address);
        time += caller.getL1d().getLatency();
        caller.getL1d().setState(index1, CacheLine.MESI.Exclusive);
        index2 = caller.getL2().add(address);
        time += caller.getL2().getLatency();
        caller.getL2().setState(index2, CacheLine.MESI.Exclusive);
        index3 = l3.add(address);
        time += l3.getLatency();
        l3.setState(index3, CacheLine.MESI.Exclusive);
        return time;
    }

    /**
     * Invalidates block in the caches of the other CPUs that contain the passed address.
     * @param address
     * @param caller
     * @return
     */
    public int issueRequestForOwnership(long address, CPU caller) {
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
            time += otherCPU.getL1i().getLatency();
            otherCPU.getL1i().setState(index1i, CacheLine.MESI.Invalid);
            stateChanges[2][3]++;   // MESI Change: Shared -> Invalid
        }
        if(index1d != -1) {
            time += otherCPU.getL1d().getLatency();
            otherCPU.getL1d().setState(index1d, CacheLine.MESI.Invalid);
            stateChanges[2][3]++;   // MESI Change: Shared -> Invalid
        }
        if(index2 != -1) {
            time += otherCPU.getL2().getLatency();
            otherCPU.getL2().setState(index2, CacheLine.MESI.Invalid);
            stateChanges[2][3]++;   // MESI Change: Shared -> Invalid
        }

        return time;
    }

    private int memWrite(long address) {
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