package model;

import java.io.PrintStream;

/**
 * The main memory of the system.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Memory {
    /**
     * If enabled, prints debugging messages to the console.
     */
    public boolean debug = false;
    /**
     * Where to print debugging messages - System.out by default.
     */
    public PrintStream debuggingOutput = System.out;

    private int reads;
    private int writes;
    private int size;
    private int readLatency;
    private int writeLatency;
    
    /**Constructor for a memory object.
     * @param size Integer to determine the size of the memory object.
     * @param readLatency The latency penalty associated with reading. Written in nanoseconds.
     * @param writeLatency The latency penalty associated with writing. Written in nanoseconds.
     */
    public Memory(int size, int readLatency, int writeLatency) {
        this.size = size;
        this.readLatency = readLatency;
        this.writeLatency = writeLatency;
    }
    /** Checks to make sure location is valid, then increments the reads counter.
     * * If debug, also prints mem location to console.
     * @param location The location of the memory address to read from.
     */
    boolean read(long location) {
        if(location < size) {
            reads++;
            if(debug) debuggingOutput.println("Memory location " + location + " read successfully.");
            return true;
        } else {
            if(debug) debuggingOutput.println("Memory location " + location + " read failed.");
            return false;
        }
    }
    /** Checks to make sure location is valid, then increments the writes counter.\
     * If debug, also prints mem location to console.
     * @param location The location of the memory address to write to.
     */
    boolean write(long location) {
        if(location < size) {
            writes++;
            if(debug) debuggingOutput.println("Memory location " + location + " written successfully.");
            return true;
        } else {
            // This will happen only with incorrect addresses.
            if(debug) debuggingOutput.println("Memory location " + location + " write failed.");
            return false;
        }
    }

    int getReadLatency() {
        return readLatency;
    }

    int getWriteLatency() {
        return readLatency;
    }

    int getReads() {
        return reads;
    }

    int getWrites() {
        return writes;
    }

    /** Returns the product of read latency and reads, summed with the product of writes and the write penalty.
     * @return The total time in ns.
     */
    int getTotalTime() {
        return reads*readLatency + writes*writeLatency;
    }
}
