/*
 * Copyright Nick Sandow 2013.
 */
package com.nicksandow.lrucache.example;

import com.nicksandow.lrucache.LruCache;

/**
 * This demonstrates usage of LruCache.java.
 * <p/>
 * This example: prime factorisation.
 */
class CachedFactoriser implements PrimeFactoriser
{
    private PrimeFactoriser factoriser = new SimpleFactoriser();
    
    private LruCache<Long, Long[]> cache;
    
    CachedFactoriser(int cacheCapacity)
    {
        cache = new LruCache<Long, Long[]>(cacheCapacity);
    }
    
    public Long[] factorise(long num)
    {
        Long[] factors = cache.read(num);
        
        if (factors != null)
        {
            // Yay!  The value for num was in the cache, so we just return it.
            return factors;
        }
        else
        {
            // The result was not in the cache - we're forced to calculate it.
            factors = factoriser.factorise(num);
            
            // Add the value into the cache.  This will cause the eviction of 
            // the LRU item if necessary (as per the LruCache contract.)
            cache.install(num, factors);
            
            return factors;
        }
    }
}
