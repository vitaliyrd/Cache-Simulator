package model;

/**
 * Created by Vitaliy on 6/3/15.
 */
public class Cache {
    private int accesses;
    private int misses;
    private int cache[];

    private int size;
    private int blockSize;
    private int associativity;
    private int latency;

    public Cache(int size, int blockSize, int associativity, int latency) {
        accesses = 0;
        misses = 0;
        cache = new int[size];

        this.size = size;
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
        for(int i = 0; i < size; i++) {
            if(false/*search*/) {
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
            index = offset & ~(0xFFFFFFFF << (int) (Math.log(size) / Math.log(2)));
            //replace whatever pointed by index
        }
    }
}
