/*
 * Copyright Nick Sandow 2013.
 */
package com.nicksandow.lrucache.example;

/**
 *
 */
public class Example 
{
    private static int CACHE_CAPACITY = 10000;
    
    public static void main(String[] args) 
    {
        if (args.length != 1)
        {
            System.err.println("Example: Usage: java Example <num requests> <cache capacity>");
        }
        
        int numRequests = Integer.parseInt(args[0]);
        int cacheCapacity = Integer.parseInt(args[1]);
        
        long start = System.currentTimeMillis();
        
        simpleFactorise(numRequests);
        
        long end = System.currentTimeMillis();
        
        System.out.println("Time to perform " + numRequests + 
                " with SimpleFactoriser is: " + (end - start) + "ms.");
        
        start = System.currentTimeMillis();
        cachedFactorise(numRequests);
        end = System.currentTimeMillis();

        System.out.println("Time to perform " + numRequests + 
                " with CachedFactoriser is: " + (end - start) + "ms.");
    }
    
    private static void simpleFactorise(int numRequests)
    {
        
    }
    
    private static void cachedFactorise(int numRequests)
    {
        
    }    
}
