package model;

import java.lang.*;
import java.lang.System;

/**
 * A memory cache, configurable to be direct-mapped or associative.
 * NOTE: The cache is undefined upon initialization; all memories must be fetched the first time the cache is used.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Cache {
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
        int index = 0; //might need this as a field
        int offset = location >> (blockSize/4);
        if (associativity > 1) {
            index = offset & ~(0xFFFFFFFF << (int)(Math.log(associativity) / Math.log(2)));
            // search the set
        } else {
            index = offset & ~(0xFFFFFFFF << (int) (Math.log(blocks) / Math.log(2)));

            int tag = location >> (blockSize / 4);                  // First truncate the offset.
            tag = tag >> (int) (Math.log(blocks) / Math.log(2));    // Truncate the index.

            if(cache[index] == tag) {
                return true;
            }
        }
        misses++;
        return false;
    }

    /**
     * Adds the passed memory location to the correct position in this Cache.
     *
     * @param location The memory address to add.
     */
    public void fetch(int location) {
        int index = 0; //might need this as a field
        int offset = location >> (blockSize/4);
        if (associativity > 1) {
            index = offset & ~(0xFFFFFFFF << (int)(Math.log(associativity) / Math.log(2)));
            //find first unused block in the set indicated by the index
            //if no free block found replace via LRU
        }
        else {
            index = offset & ~(0xFFFFFFFF << (int) (Math.log(blocks) / Math.log(2)));

            int tag = location >> (blockSize / 4);                  // First truncate the offset.
            tag = tag >> (int) (Math.log(blocks) / Math.log(2));    // Truncate the index.

            cache[index] = tag;
        }
    }
}
