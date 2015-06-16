package model;

/**
 * Wrapper class for a cache line.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class CacheLine {
    public long tag;
    /**
     * True if valid.
     */
    public boolean valid;
    /**
     * True if Modified.
     */
    public boolean dirty;
    /**
     * True if shareable, false if exclusive.
     */
    public boolean shared;

    public CacheLine() {
        tag = 0;
        valid = false;
        dirty = false;
        shared = false;
    }

    public boolean isExclusive() {
        return !shared;
    }

    public boolean isShared() {
        return shared;
    }

    public boolean isInvalid() {
        return !valid;
    }

    public boolean isModified() {
        return dirty;
    }

    public enum MESI {Modified,Exclusive,Shared,Invalid}
}
