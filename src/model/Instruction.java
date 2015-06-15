package model;

/**
 * Simple wrapper for instructions that can get passed to the CPUs.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Instruction {
    /**
     * Representaion of an opcode.
     */
    public int instruction;
    /**
     * Enum object to flag if the Action is Read or write.
     */
    public MemoryAction memoryAction;
    /**
     * Dont actually care what the data is, just need something.
     */
    public int data;
    
    /**
     * Just functions as a flag.
     */
    public enum MemoryAction {
        READ, WRITE
    }
}
