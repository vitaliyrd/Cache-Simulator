package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vitaliy on 6/3/15.
 */
public class Cache {
    private int accesses;
    private int misses;
    private List<Integer> contents;

    private int size;
    private int blockSize;
    private int latency;

    public Cache(int size, int blockSize, int latency) {
        accesses = 0;
        misses = 0;
        contents = new ArrayList<>();

        this.size = size;
        this.blockSize = blockSize;
        this.latency = latency;
    }
}
