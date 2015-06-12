package model;

/**
 * Created by Vitaliy on 6/12/15.
 */
public class Instruction {
    public int instruction;
    public MemoryAction memoryAction;
    public int data;

    public enum MemoryAction {
        READ, WRITE, NONE;
    }
}