package model;

/**
 * The main memory of the system.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Memory {
    public boolean debug;

    private int reads;
    private int writes;
    private int size;
    private int readLatency;
    private int writeLatency;
    
    /**Constructor for a memory object.
     * @param Size Integer to determine the size of the memory object.
     * @param readLatency The latency penalty associated with reading. Written in nanoseconds.
     * @param writeLatency The latency penalty associated with writing. Written in nanoseconds.
     */
    public Memory(int size, int readLatency, int writeLatency) {
        this.size = size;
        this.readLatency = readLatency;
        this.writeLatency = writeLatency;
    }
    /** Checks to make sure location is valid, then increments the reads counter.
     * @param location The location of the memory address to read from.
     */
    boolean read(int location) {
        if(location < size) {
            reads++;
            if(debug) System.out.println("Memory location " + location + " read successfully.");
            return true;
        } else {
            if(debug) System.out.println("Memory location " + location + " read failed.");
            return false;
        }
    }
    /** Checks to make sure location is valid, then increments the writess counter.
     * @param location The location of the memory address to write to.
     */
    boolean write(int location) {
        if(location < size) {
            writes++;
            if(debug) System.out.println("Memory location " + location + " written successfully.");
            return true;
        } else {
            // This will happen only with incorrect addresses.
            if(debug) System.out.println("Memory location " + location + " write failed.");
            return false;
        }
    }
    /** Returns the product of read latency and reads, summed with the product of writes and the write penalty.
     * @return The total time in ns.
     */
    int getTotalTime() {
        return reads*readLatency + writes*writeLatency;
    }
}
