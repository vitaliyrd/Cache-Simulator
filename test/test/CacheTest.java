package test;

import junit.framework.TestCase;
import model.Cache;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Vitaliy on 6/11/15.
 */
public class CacheTest extends TestCase {
    private Cache cache;


    @Before
    public void setup() {
    }

    @Test
    public void testDirectMappedCache() {
        int blocks = 512;
        int blockSize = 64;
        int associativity = 4; //Why are we setting associativity to 4 if it's direct mapped? (Alex)
        cache = new Cache(blocks, blockSize, associativity, 1);

        cache.fetch(0b01111101011101110001101100111000);
        cache.locate(0b01111101011101110001101100111000);   // Hit
        cache.locate(0b01111101011101110001101100111010);   // Hit
        cache.locate(0b01111101011101110001101101111000);   // Miss
        cache.locate(0b01111101111101110001101100111000);   // Miss

        cache.fetch(0b01111101011101110001111111111000);
        cache.locate(0b01111101011101110001111111111000);   // Hit
        cache.locate(0b01111101011101110001111111111000);   // Hit
        cache.locate(0b01101101011101110001111111111000);   // Miss
        cache.locate(0b01111101011101110001101111111000);   // Miss

    }

    @Test
    public void testAssociativeCache() {

    }

    @Test
    public void testFullyAssociativeCache() {

    }
}
