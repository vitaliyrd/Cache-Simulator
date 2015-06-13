package model;

import java.lang.*;
import java.util.Random;

/**
 * A memory cache, configurable to be direct-mapped or associative.
 * The Cache is assumed to have symmetric (read == write) latencies.
 * NOTE: The cache is undefined upon initialization; all memories must be fetched the first time the cache is used.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Cache {
    private static Random generator = new Random();

    public boolean debug = false;   // Prints debug messages to the console.

    /**
     * Counts the number of accesses of this Cache.
     * Accesses include: locate and write operations.
     * Note: Fetches will not be included in this count since its assumed that memory will be fetched upon a miss.
     */
    private int accesses = 0;
    /**
     * Counts the number of misses of this Cache.
     * Misses can only occur on locate calls.
     */
    private int misses = 0;
    /**
     * Counts the number of hits of this Cache.
     * Misses can only occur on locate calls.
     */
    private int hits = 0;
    /**
     * Counts the number of writes in this Cache.
     * Writes can only occur on write calls.
     */
    private int writes = 0;

    private int cache[];

    private int blocks;
    private int blockSize;
    private int associativity;
    private int latency;

    public Cache(int blocks, int blockSize, int associativity, int latency) {
        cache = new int[blocks];

        this.blocks = blocks;
        this.blockSize = blockSize;
        this.associativity = associativity;
        this.latency = latency;
    }

    /**
     * Searches for the passed memory location returns whether it is in the Cache.
     *
     * @param location The memory address to search for.
     * @return True if the search was a hit, false if the search was a miss.
     */
    public boolean locate(int location) {
    	accesses++;

        int offsetBits = (int)(Math.log(blockSize) / Math.log(2));
        // This is the memory address with the bits representing the offset truncated.
        int offsetRemoved = location >>> offsetBits;  // The >>> prevents sign extension.

        // If the cache is a direct-mapped cache:
        if(associativity == 1) {
            int indexBits = (int)(Math.log(blocks) / Math.log(2));
            int index = offsetRemoved & ~(0xFFFFFFFF << indexBits);
            int tag = location >>> indexBits + offsetBits;  // The >>> prevents sign extension.

            if(cache[index] == tag) {
                if(debug) System.out.println("Tag " + tag + " located in line " + index + " of cache.");

                hits++;
                return true;
            }
        } else {
            int setBits = (int)(Math.log(blocks / associativity) / Math.log(2));
            int set = offsetRemoved & ~(0xFFFFFFFF << setBits);
            int tag = location >>> setBits + offsetBits;    // The >>> prevents sign extension.

            for(int i = 0; i < associativity; i++) {
                if(cache[(set * associativity) + i] == tag) {
                    if(debug) System.out.println("Tag " + tag + " located in set " + set + ", index " + i + " of cache.");

                    hits++;
                    return true;
                }
            }
        }

        misses++;
        if(debug) System.out.println("Cache Miss");
        return false;
    }

    /**
     * Returns the index in the cache where this address can be found.
     * Warning: This method assumes that the same address has been tested for a hit with the locate method.
     * This method is really only needed for shared caches.
     *
     * @param location The address to look for.
     * @return The index of the passed address.
     */
    public int getIndex(int location) {
        int offsetBits = (int)(Math.log(blockSize) / Math.log(2));
        // This is the memory address with the bits representing the offset truncated.
        int offsetRemoved = location >>> offsetBits;  // The >>> prevents sign extension.

        // If the cache is a direct-mapped cache:
        if(associativity == 1) {
            int indexBits = (int)(Math.log(blocks) / Math.log(2));
            int index = offsetRemoved & ~(0xFFFFFFFF << indexBits);

            return index;

        // If the cache is an associative cache:
        } else {
            int setBits = (int)(Math.log(blocks / associativity) / Math.log(2));
            int set = offsetRemoved & ~(0xFFFFFFFF << setBits);
            int tag = location >>> setBits + offsetBits;    // The >>> prevents sign extension.

            for(int i = 0; i < associativity; i++) {
                if(cache[(set * associativity) + i] == tag) {
                    return (set * associativity) + i;
                }
            }
        }
        return -1; // Under normal execution, this should never happen.
    }

    /**
     * Adds the passed memory location to the correct position in the Cache.
     *
     * @param location The memory address to add.
     */
    public void fetch(int location) {
        int offsetBits = (int)(Math.log(blockSize) / Math.log(2));
        // This is the memory address with the bits representing the offset truncated.
        int offsetRemoved = location >>> offsetBits;  // The >>> prevents sign extension.

        // If the cache is a direct-mapped cache:
        if(associativity == 1) {
            int indexBits = (int)(Math.log(blocks) / Math.log(2));
            int index = offsetRemoved & ~(0xFFFFFFFF << indexBits);
            int tag = location >>> indexBits + offsetBits;  // The >>> prevents sign extension.

            cache[index] = tag;
            if(debug) System.out.println("Tag " + tag + " added to line " + index + " of cache.");
        }

        // If the cache is an associative cache:
        else {
            int setBits = (int)(Math.log(blocks / associativity) / Math.log(2));
            int set = offsetRemoved & ~(0xFFFFFFFF << setBits);
            int tag = location >>> setBits + offsetBits;    // The >>> prevents sign extension.

            // For now it's inserting at random, as the instructions don't specify whether we are to do
            // LRU or Random. Random is easier to implement.
            int index = generator.nextInt(associativity);

            cache[(associativity * set) + index] = tag;

            if(debug) System.out.println("Tag " + tag + " added to set " + set + ", index " + index + " of cache.");
        }
    }

    public void write(int address) {
        accesses++;
        writes++;
        fetch(address);     // This write is also found at the memory in address, so we should keep things in sync.
    }

    public int getMisses() {
        return misses;
    }

    public int getHits() {
        return accesses - misses;
    }

    public int getAccesses() {
        return accesses;
    }

    public int getLatency() {
        return latency;
    }

    /**
     * Returns the total time the CPU had to wait on this Cache.
     *
     * @return Total time in nanoseconds.
     */
    public int getTotalTime() {
        return accesses * latency;
    }

    private enum MESI {
        /**
         * This cache line is present only in the this cache, and is dirty; it does not match main memory.
         */
        MODIFIED,
        /**
         * This cache line is present only in the this cache, and is clean; it matches main memory.
         */
        EXCLUSIVE,
        /**
         * This cache line may be stored in other caches and is clean; it matches main memory.
         */
        SHARED,
        /**
         * This cache line is invalid (unused).
         */
        INVALID;
    }
}
