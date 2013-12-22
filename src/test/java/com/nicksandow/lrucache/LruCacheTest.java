/*
 * Copyright Nick Sandow 2013.
 */
package com.nicksandow.lrucache;

import junit.framework.TestCase;
import static junit.framework.Assert.*;

/**
 * Tests for LruCache.
 * 
 * For this unit test, we are caching the result of a simple function: 
 * fn(x) = x + 1000.
 * 
 * Keys are ints in the range [0, CACHE_SIZE) and values are [1000, CACHE_SIZE 
 * + 1000).
 */
public class LruCacheTest extends TestCase 
{
    private LruCache<Integer, Integer> cache;
    
    private static final int CACHE_SIZE = 5;

    public LruCacheTest(String testName) 
    {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception 
    {
        super.setUp();

        cache = new LruCache<Integer, Integer>(CACHE_SIZE);
    }
    
    @Override
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }

    /**
     * Test that a new cache is indeed empty.
     */
    public void testEmptyCache()
    {
        for (int i = 0; i < 10; i++)
        {
            Integer val = cache.read(i);
            assertNull(val);
        }
    }

    /**
     * Test that we can fill a cache with {@code install} and get the same values
     * back out with {@code read}.
     */
    public void testSetAndGetEntry() 
    {
        fillCache();
        
        for (int i = 0; i < CACHE_SIZE; i++)
        {
            assertTrue(i + 1000 == cache.read(i));
        }
    }

    /**
     * Test that the right item is evicted (according to the LRU policy) when 
     * we add more items than can fit.
     */
    public void testEviction()
    {
        fillCache();
        
        assertEquals(CACHE_SIZE, cache.getCurrentSize());
        
        // Cache is now full.  Add one more.
        cache.install(CACHE_SIZE, CACHE_SIZE + 1000);
        
        // According to LRU policy, the first added entry should have been
        // evicted.  Check this.
        assertNull(cache.read(0));
    }

    public void testStats()
    {
        fillCache();
        
        for (int i = 0; i < CACHE_SIZE; i++)
        {
            int value = cache.read(i);
        }
        
        // We should have exactly {@code CACHE_SIZE} hits.
        assertEquals(CACHE_SIZE, cache.getStats().getHits());
        
        for (int i = 0; i < CACHE_SIZE; i++)
        {
            cache.write(i, i + 1000);
        }        

        // We should have exactly 2 * {@code CACHE_SIZE} hits.
        assertEquals(2 * CACHE_SIZE, cache.getStats().getHits());
        
        // Now do some writes that will be misses.
        for (int i = 0; i < CACHE_SIZE; i++)
        {
            cache.write(i + CACHE_SIZE, i + 1000);
        }        
        
        // We should have exactly 2 * {@code CACHE_SIZE} hits, and
        // exactly {@code CACHE_SIZE} misses.
        assertEquals(2 * CACHE_SIZE, cache.getStats().getHits());
        assertEquals(CACHE_SIZE, cache.getStats().getMisses());
    }
    
    /**
     * A helper that fills the cache.
     */
    private void fillCache()
    {
        for (int i = 0; i < CACHE_SIZE; i++)
        {
            cache.install(i, i + 1000);
        }        
    }

}
