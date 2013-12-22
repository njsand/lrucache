/*
 * Copyright Nick Sandow 2013.
 */
package com.nicksandow.lrucache.example;

/**
 * Defines an interface for finding the prime factorisation of a Long value.
 */
interface PrimeFactoriser
{
    /**
     * Return the prime factors of {@param num}.
     */
    Long[] factorise(long num);
        
}
