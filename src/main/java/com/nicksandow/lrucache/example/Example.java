/*
 * Copyright Nick Sandow 2013.
 */
package com.nicksandow.lrucache.example;

import java.util.Arrays;

/**
 * 
 */
class Example 
{ 
    private static final long TARGET_NUMBER_BASE = 1000000000L;
    private static final long TARGET_NUMBER_RANGE = 20L;
    
    public static void main(String[] args) 
    {
        int numRequests = 100;
        int cacheCapacity = 500;
        
        switch (args.length)
        {
            case 2:
                cacheCapacity = Integer.parseInt(args[1]);
                // fall through
                
            case 1:
                numRequests = Integer.parseInt(args[0]);
                // fall through
                
            case 0:
                break;
                
            default:    
                System.err.println("Example: Usage: java Example <num requests> <cache capacity>");
        }
        
        long time = factorise(new SimpleFactoriser(), numRequests);
        
        System.out.println("Time to perform " + numRequests + 
                " with SimpleFactoriser is: " + time + "ms.");
        
        time = factorise(new CachedFactoriser(cacheCapacity), numRequests);
        
        System.out.println("Time to perform " + numRequests + 
                " with CachedFactoriser is: " + time + "ms.");
    }
    
    private static long factorise(PrimeFactoriser factoriser, int num)
    {
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < num; i++)
        {
            Long[] target = factoriser.factorise(TARGET_NUMBER_BASE + 
                    (long)(Math.random() * TARGET_NUMBER_RANGE));
            
            System.out.println(Arrays.toString(target));
        }
        
        long end = System.currentTimeMillis();
        
        return end - start;
    }
}
