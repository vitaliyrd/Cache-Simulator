package model;

import java.lang.*;
import java.lang.System;
import java.util.Random;

/**
 * A memory cache, configurable to be direct-mapped or associative.
 * NOTE: The cache is undefined upon initialization; all memories must be fetched the first time the cache is used.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Cache {
    private static Random generator = new Random();

    private int accesses;
    private int misses;
    private int cache[];

    private int blocks;
    private int blockSize;
    private int associativity;
    private int latency;

    public Cache(int blocks, int blockSize, int associativity, int latency) {
        accesses = 0;
        misses = 0;
        cache = new int[blocks];

        this.blocks = blocks;
        this.blockSize = blockSize;
        this.associativity = associativity;
        this.latency = latency;
    }

    /**
     * Searches for a memory location to determine whether it is in this Cache.
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
                System.out.println("Tag " + tag + " located in line " + index + " of cache.");
                return true;
            }
        } else {
            int setBits = (int)(Math.log(blocks / associativity) / Math.log(2));
            int set = offsetRemoved & ~(0xFFFFFFFF << setBits);
            int tag = location >>> setBits + offsetBits;    // The >>> prevents sign extension.

            for(int i = 0; i < associativity; i++) {
                if(cache[(set * associativity) + i] == tag) {
                    System.out.println("Tag " + tag + " located in set " + set + ", index " + i + " of cache.");
                    return true;
                }
            }
        }
        misses++;
        System.out.println("Cache Miss");
        return false;
    }

    /**
     * Adds the passed memory location to the correct position in this Cache.
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
            System.out.println("Tag " + tag + " added to line " + index + " of cache.");
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
            System.out.println("Tag " + tag + " added to set " + set + ", index " + index + " of cache.");
        }
    }
}
