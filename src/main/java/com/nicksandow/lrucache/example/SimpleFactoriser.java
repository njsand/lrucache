/*
 * Copyright Nick Sandow 2013.
 */
package com.nicksandow.lrucache.example;

import java.util.ArrayList;

/**
 * A simple implementation of {@link PrimeFactoriser}.
 */
class SimpleFactoriser implements PrimeFactoriser
{
    public Long[] factorise(long num)
    {
        ArrayList<Long> factors = new ArrayList<Long>();
    
        for (long i = 2; i <= num / i; i++) 
        {
            while (num % i == 0) 
            {
                factors.add(i);
                num /= i;
            }
        }
        
        if (num > 1) 
        {
            factors.add(num);
        }
        
        try 
        {
            // Add in a bit of a delay to simulate a lengthy task.
            Thread.sleep(50);
        } 
        catch (InterruptedException ex) 
        {
            // Ignore.
        }
        
        return factors.toArray(new Long[]{});
    }
}
