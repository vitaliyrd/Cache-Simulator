package model;

/**
 * Wrapper class for a cache line.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class CacheLine {
    public int tag;
    public boolean valid;
    public boolean dirty;
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
}
