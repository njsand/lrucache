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

     // The cache will never store more entries than this.
    private final int capacity;
    
    // The current size of the cache.
    private int currentSize;

    // References to the head and tail of the LRU list.  We do not use
    // java.util.LinkedList for this list - instead we manage the links
    // ourselves.  The reason for this is we need to be able to grab potentially
    // any element of the list and put it on the head of the list 
    // (on a cache hit).  This is tricky to do with LinkedList...
    private Entry<K, V> lruListHead;
    private Entry<K, V> lruListTail;

    private final Stats stats;
    
    public LruCache(int capacity)
    {
        this.capacity = capacity;
        currentSize = 0;
        lruListHead = null;
        lruListTail = null;
        stats = new Stats();
    }

    /**
     * Add the key/value pair to the cache.
     * <p>
     * If the current size of the cache is larger than capacity, the least
     * recently used pair will be evicted (After a key/value pair is evicted,
     * any {@link #read} with that key will return null).
     * <p>
     * A key/value pair is considered "used" in this sense if it is {@link
     * #read} from the cache or added by either the {@link #install} or {@link
     * #write} methods.
     * 
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
     * Add the key/value pair to the cache but do no hit/miss stats.
     * 
     * This does exactly the same thing as {@link #write}, except that it has no
     * effect on the cache stats.  The point of this is that after a failed read
     * (which {@emph does} affect the miss rate) you can call this method to place the
     * key/value into the cache.  We don't want to record a second, spurious
     * cache miss in this case - hence the reason for this method.
     * 
     * @param key The key to add to the cache.
     * @param value The value to add to the cache.
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
            if (currentSize < capacity)
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
     * Lookup the value corresponding to {@code key}.
     * 
     * @param key
     * @return The value corresponding to {@code key}, or {@code null} if it is
     * not present.
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
     * @return the number of entries in the cache.  This will be an integer in
     * the range [0, capacity], where capacity was fixed at the construction of
     * this cache.
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
     * This puts the parameter {@code entry} at the head of the LRU list.
     * 
     * @param entry The node to move.
     */
    private void updateLru(Entry entry)
    {
        if (currentSize == 1)
        {
            // A list of size 1; do nothing
        }
        else
        {
            if (entry == lruListHead)
            {
                // It's already at the head, so do nothing.
            }
            else
            {
                // This could probably be simpler here.
                if (entry == lruListTail)
                {
                    // Set the new tail
                    entry.prev.next = null;
                    lruListTail = entry.prev;
                }
                else
                {
                    // It's somewhere in the middle.  Remove it.
                    entry.prev.next = entry.next;
                    entry.next.prev = entry.prev;
                }

                // Put it at the head of the list.
                entry.prev = null;
                entry.next = lruListHead.next;
                lruListHead.prev = entry;
                lruListHead = entry;
            }
        }
    }

    // Returns the entry that was evicted (and is now free for re-use).
    private Entry evict()
    {
        nameToValueMap.remove(lruListTail.key);

        // Because the LRU list is sorted (by decreasing recency of access)
        // we know the tail is the LRU item - thus we simply return that.
        return lruListTail;
    }

    // Add a new entry to the cache.
    //
    // Pre-condition: There must be space for the entry.  This method does
    // no checking for free space.
    private void addNewEntry(K key, V value)
    {
        Entry newNode = new Entry(key, value, lruListHead, null);
        
        if (currentSize != 0)
        {
            lruListHead.prev = newNode;
        }
        else
        {
            lruListTail = newNode;
        }

        lruListHead = newNode;
        
        ++currentSize;

        nameToValueMap.put(key, newNode);
    }

    private void setCacheEntry(Entry entry, K name, V value)
    {
        entry.key = name;
        entry.value = value;

        nameToValueMap.put(name, entry);
    }
    
    /**
     * For debug.  Probably should be removed at some point.
     */
    void dumpCache()
    {
        System.out.println("cache stats:");

        System.out.println("capacity: " + capacity);
        System.out.println("current size: " + currentSize);

        System.out.println("LRU list:");
        Entry nextEntry = lruListHead;

        while (nextEntry != null)
        {
            System.out.print("(" + nextEntry.key + ", " + nextEntry.value + ") -> ");

            nextEntry = nextEntry.next;
        }
    }    

    /**
     * An entry in the cache.  Instances of these are chained in a linked list
     * that is the LRU list.
     * 
     * @param <K> The type of the keys in the cache.
     * @param <V> The type of the values.
     */
    private static class Entry<K, V>
    {
        Entry(K name, V value, Entry next, Entry prev)
        {
            this.key = name;
            this.value = value;
            this.next = next;
            this.prev = prev;
        }

        K key;
        V value;

        // The next node in the LRU list.
        Entry next;
        // The previous node in the LRU list.
        Entry prev;

        @Override
        public String toString()
        {
            return "name: " + key + " value: " + value + " next: " + next + " prev: " + prev;
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

