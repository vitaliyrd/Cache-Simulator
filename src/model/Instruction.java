package model;

/**
 * Simple wrapper for instructions that can get passed to the CPUs.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Instruction {
    public int instruction;
    public MemoryAction memoryAction;
    public int data;
    
    /**
     * Just functions as a flag.
     */
    public enum MemoryAction {
        READ, WRITE
    }
}
