package test;

import junit.framework.TestCase;
import model.Cache;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the Cache class.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
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
        int associativity = 1;
        cache = new Cache(blocks, blockSize, associativity, 1);
        cache.debug = true;

        int index = cache.add(0b01111101011101110001101100111000);
        cache.markValid(index);
        cache.locate(0b01111101011101110001101100111000);   // Hit
        cache.locate(0b01111101011101110001101100111010);   // Hit
        cache.locate(0b01111101011101110001101101111000);   // Miss
        cache.locate(0b01111101111101110001101100111000);   // Miss

        System.out.println();

        index = cache.add(0b01111101011101110001111111111000);
        cache.markValid(index);
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
