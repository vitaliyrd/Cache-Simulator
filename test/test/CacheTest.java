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
        int blocks = 32;
        int blockSize = 16;
        int associativity = 1;
        cache = new Cache(blocks, blockSize, associativity, 1);


    }

    @Test
    public void testAssociativeCache() {

    }

    @Test
    public void testFullyAssociativeCache() {

    }
}