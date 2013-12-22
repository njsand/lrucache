/*
 * Copyright Nick Sandow 2013.
 */
package com.nicksandow.lrucache;

import java.util.Map;
import java.util.HashMap;

/**
 * A generic, fixed size LRU (least recently used) cache.  When deciding to evict an 
 * item from the cache, the key that has least recently been written or read is evicted.
 * <p/>
 * Internally, the implementation consists of 2 data structures - a hash map that stores
 * the mapping from key to value, and a list that orders keys by recency
 * of access (with the key most recently accessed on the head of the list.)
 * <p/>
 * This class performs no synchronisation and is not thread safe.
 */
public class LruCache<K, V>
{
    private Map<K, Entry<K, V>> nameToValueMap = new HashMap<K, Entry<K, V>>();

    private int currentSize;

    // References to the head and tail of the LRU list.  We do not use
    // java.util.LinkedList for this list - instead we manage the links
    // ourselves.  The reason for this is we need to be able to grab potentially
    // any element of the list and put it on the head of the list 
    // (on a cache hit).  This is tricky to do with LinkedList...
    private Entry<K, V> lruListHead;
    private Entry<K, V> lruListTail;

    private Entry[] entries;

    private Stats stats;
    
    public LruCache(int capacity)
    {
        entries = new Entry[capacity];

        lruListHead = null;
        lruListTail = null;

        currentSize = 0;

        stats = new Stats();
    }
    
    /**
     * Add the {@code key}/{@code value} pair to the cache.
     * 
     * The difference between this and {@link #write} is that this method
     * never records a cache miss.  If the key is not present, it is added
     * in exactly the same manner as the {@link #write} method, but no cache 
     * miss will be recorded.
     * 
     * @param key
     * @param value 
     */
    public boolean install(K key, V value)
    {
        Entry entry = nameToValueMap.get(key);

        // Is it in the cache?
        if (entry != null)
        {
            // Yes. We just update the value and LRU info and we're done.
            entry.value = value;
            updateLru(entry);
            
            return true;
        }
        else  // Not in cache.
        {
            if (currentSize < entries.length)
            {
                // There is still capacity in the cache, so just add it in.
                // No need to evict anything.
                addNewEntry(key, value);
            }
            else
            {
                // No capacity left, so we need to evict the LRU entry.
                Entry evicted = evict();
                setCacheEntry(evicted, key, value);
                updateLru(evicted);
            }
            
            return false;
        }
    }

    /**
     * The difference between this and {@link #write} is that 
     * @param key
     * @param value 
     */
    public void write(K key, V value)
    {
        boolean wasPresent = install(key, value);
        
        if (wasPresent)
        {
            stats.incHits();
        }
        else
        {
            stats.incMisses();
        }
    }
    
    /**
     * Lookup the entry corresponding to {@code key}.
     * 
     * @param key
     * @return The instance of {@code K} corresponding to {@code key}, otherwise
     * null.
     */
    public V read(K key)
    {
        Entry<K, V> entry = nameToValueMap.get(key);

        if (entry == null)
        {
            // Cache miss!
            stats.incMisses();
            
            return null;
        }
        else
        {
            // Cache hit!
            stats.incHits();
            updateLru(entry);

            return entry.value;
        }
    }

    /**
     * This puts the entry {@code e} at the head of the LRU list.
     * 
     * @param e 
     */
    private void updateLru(Entry e)
    {
        if (currentSize == 1)
        {
            // A list of size 1; do nothing
        }
        else
        {
            if (e == lruListHead)
            {
                // It's already at the head, so do nothing.
            }
            else
            {
                // This could probably be simpler here.
                
                if (e == lruListTail)
                {
                    // Set the new tail
                    entries[e.prev].next = -1;
                    lruListTail = entries[e.prev];
                }
                else
                {
                    // It's somewhere in the middle.  Remove it.
                    entries[e.prev].next = e.next;
                    entries[e.next].prev = e.prev;
                }

                // Put it at the head of the list.
                e.prev = -1;
                e.next = lruListHead.pos;
                lruListHead.prev = e.pos;
                lruListHead = e;
            }
        }
    }

    // Returns the entry that was evicted (and is now free).
    private Entry evict()
    {
        nameToValueMap.remove(lruListTail.key);

        // Because the LRU list is sorted (by decreasing recency of access)
        // we know the tail is the LRU item - thus we simply return that.
        return lruListTail;
    }

    // Presumes there is space.  This is only used to fill the cache initially.
    private void addNewEntry(K key, V value)
    {
        Entry newNode = null;

        if (currentSize == 0)
        {
            newNode = entries[0] = new Entry(key, value, currentSize, -1, -1);
            lruListHead = lruListTail = entries[0];
        }
        else
        {
            // Add it to end then adjust.
            newNode = entries[currentSize] = new Entry(key, value, currentSize, -1, lruListTail.pos);
            lruListTail = entries[currentSize];
        }

        ++currentSize;
        updateLru(newNode);

        nameToValueMap.put(key, newNode);
    }

    private void setCacheEntry(Entry slot, K name, V value)
    {
        slot.key = name;
        slot.value = value;

        nameToValueMap.put(name, slot);
    }
    
    /**
     * @return the number of entries in the cache.  This will be an integer
     * in the range [0, capacity], where the capacity is fixed and was
     * determined upon construction of this cache.
     */
    public int getCurrentSize()
    {
        return currentSize;
    }

    /**
     * @return A new instance of {@code Stats} that contains cache statistics
     * such as hit/miss information.
     */
    public Stats getStats()
    {
        return new Stats(stats);
    }
    
    /**
     * For debug.  Probably should be removed at some point.
     */
    void dumpCache()
    {
        System.out.println("cache stats:");

        System.out.println("capacity: " + entries.length);
        System.out.println("current size: " + currentSize);

        System.out.println("LRU list:");
        int nextSlot = lruListHead.pos;

        while (nextSlot != -1)
        {
            System.out.print("(" + entries[nextSlot].key + ", " + entries[nextSlot].value + ") -> ");

            nextSlot = entries[nextSlot].next;
        }
    }    

    private static class Entry<K, V>
    {
        Entry(K name, V value, int pos, int next, int prev)
        {
            this.key = name;
            this.value = value;
            this.pos = pos;
            this.next = next;
            this.prev = prev;
        }

        K key;
        V value;

        final int pos;
        // The "pointer" to the next node in the LRU list.  We are forced to do this because Java's
        // linked list type strangely doesn't expose the link node type...
        int next;
        // The previous "pointer"
        int prev;

        @Override
        public String toString()
        {
            return "name: " + key + " value: " + value + " pos: " + pos + " next: " + next + " prev: " + prev;
        }
    }

    /**
     * Stores basic cache statistics such as number of hits and misses, etc.
     * <p/>
     * This could be split out into its own file, but let's not do that for now.
     */
    public static class Stats
    {
       private int hits;
       private int misses;
       
       Stats(Stats toClone)
       {
           hits = toClone.hits;
           misses = toClone.misses;
       }

       private Stats() 
       {
           hits = misses = 0;
       }

       void incHits()
       {
           ++hits;
       }
       
       void incMisses()
       {
           ++misses;
       }
       
       public int getHits()
       {
           return hits;
       }
       
       public int getMisses()
       {
           return misses;
       }
       
       @Override
       public String toString()
       {
           return "Cache stats: hits: " + hits + ", misses: " + misses;
       }
    }
} 

