package model;

import java.io.PrintStream;
import java.util.Random;

/**
 * A memory cache, configurable to be direct-mapped or associative.
 * The Cache is assumed to have symmetric (read == write) latencies.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Cache {
    /**
     * If enabled, prints debugging messages to the console.
     */
    public boolean debug = false;
    /**
     * Where to print debugging messages - System.out by default.
     */
    public PrintStream debuggingOutput = System.out;

    private static Random generator = new Random();

    private int accesses = 0;
    private int misses = 0;

    private CacheLine cache[];

    private int blocks;
    private int blockSize;
    private int associativity;
    private int latency;

    public Cache(int blocks, int blockSize, int associativity, int latency) {
        cache = new CacheLine[blocks];
        for(int i = 0; i < blocks; i++) {
            cache[i] = new CacheLine();
        }

        this.blocks = blocks;
        this.blockSize = blockSize;
        this.associativity = associativity;
        this.latency = latency;
    }

    public int getLatecy() {
        return latency;
    }

    public int getMisses() {
        return misses;
    }

    public int getHits() {
        return  accesses - misses;
    }

    public int getAccesses() {
        return accesses;
    }

    /**
     * Finds and returns the index of the block containing the passed memory address if and only if the block is valid.
     *
     * @param address The memory address to search for.
     * @return Index of block containing memory address in cache or -1 if address was not found or is invalid.
     */
    public int locate(int address) {
        accesses++;

        int offsetBits = (int)(Math.log(blockSize) / Math.log(2));
        // This is the memory address with the bits representing the offset truncated.
        int offsetRemoved = address >>> offsetBits;  // The >>> prevents sign extension.

        // If the cache is a direct-mapped cache:
        if(associativity == 1) {
            int indexBits = (int)(Math.log(blocks) / Math.log(2));
            int index = offsetRemoved & ~(0xFFFFFFFF << indexBits);
            int tag = address >>> indexBits + offsetBits;  // The >>> prevents sign extension.

            if(cache[index].tag == tag && cache[index].valid) {
                if(debug) debuggingOutput.println("Tag " + tag + " located in line " + index + " of cache.");

                return index;
            }
        } else {
            int setBits = (int)(Math.log(blocks / associativity) / Math.log(2));
            int set = offsetRemoved & ~(0xFFFFFFFF << setBits);
            int tag = address >>> setBits + offsetBits;    // The >>> prevents sign extension.

            for(int i = 0; i < associativity; i++) {
                if(cache[(set * associativity) + i].tag == tag && cache[(set * associativity) + i].valid) {
                    if(debug) debuggingOutput.println("Tag " + tag + " located in set " + set + ", index " + i + " of cache.");

                    return (set * associativity) + i;
                }
            }
        }

        misses++;
        if(debug) debuggingOutput.println("Cache Miss");
        return -1;
    }

    /**
     * Adds the passed memory address into the cache if it doesn't exist already.
     * For an associative cache, the Random replacement policy is used.
     *
     * Note: Does not change status bits.
     * It is assumed that the cache controller will mark all status bits as necessary.
     *
     * @param address The memory address to add.
     * @return Index of block containing memory address that was just added.
     */
    public int add(int address) {
        // If it already exists in the cache, simply return the index where.
        int index = locate(address);
        if(index != -1 && cache[index].valid) {
            if(debug) debuggingOutput.println("A valid copy already exists at index " + index + " of cache.");
            return index;
        }

        int offsetBits = (int)(Math.log(blockSize) / Math.log(2));
        // This is the memory address with the bits representing the offset truncated.
        int offsetRemoved = address >>> offsetBits;  // The >>> prevents sign extension.

        // If the cache is a direct-mapped cache:
        if(associativity == 1) {
            int indexBits = (int)(Math.log(blocks) / Math.log(2));
            index = offsetRemoved & ~(0xFFFFFFFF << indexBits);
            int tag = address >>> indexBits + offsetBits;  // The >>> prevents sign extension.

            // TODO: Implement writeback here
            cache[index].tag = tag;
            if(debug) debuggingOutput.println("Tag " + tag + " added to line " + index + " of cache.");
            return index;
        }

        // If the cache is an associative cache:
        else {
            int setBits = (int)(Math.log(blocks / associativity) / Math.log(2));
            int set = offsetRemoved & ~(0xFFFFFFFF << setBits);
            int tag = address >>> setBits + offsetBits;    // The >>> prevents sign extension.

            // For now it's inserting at random, as the instructions don't specify whether we are to do
            // LRU or Random. Random is easier to implement.
            index = generator.nextInt(associativity);

            // TODO: Implement writeback here
            cache[(associativity * set) + index].tag = tag;

            if(debug) debuggingOutput.println("Tag " + tag + " added to set " + set + ", index " + index + " of cache.");
            return (associativity * set) + index;
        }
    }

    /**
     * Marks the line in the cache at the passed index as invalid.
     *
     * @param index The index of the cache line to invalidate.
     */
    public void markInvalid(int index) {
        cache[index].valid = false;
    }

    /**
     * Marks the line in the cache at the passed index as valid.
     *
     * @param index The index of the cache line to validate.
     */
    public void markValid(int index) {
        cache[index].valid = true;
    }

    /**
     * Marks the line in the cache at the passed index as shared.
     *
     * @param index The index of the cache line to share.
     */
    public void markShared(int index) {
        cache[index].shared = true;
    }

    /**
     * Marks the line in the cache at the passed index as shared.
     *
     * @param index The index of the cache line to mark as Exclusive.
     */
    public void markExclusive(int index) {
        cache[index].shared = false;
    }

    /**
     * Marks the line int he cache at the passed index as dirty (modified).
     *
     * @param index The index of the cache line that was modified.
     */
    public void markModified(int index) {
        cache[index].dirty = true;
    }

    /**
     * Marks the line int he cache at the passed index as not dirty (unmodified).
     *
     * @param index The index of the cache line that is not modified.
     */
    public void markNotModified(int index) {
        cache[index].dirty = false;
    }

    public boolean isModified(int index) {
        return cache[index].dirty;
    }

    public boolean isExclusive(int index) {
        return !cache[index].shared;
    }

    public boolean isShared(int index) {
        return cache[index].shared;
    }

    public boolean isInvalid(int index) {
        return !cache[index].valid;
    }
}
