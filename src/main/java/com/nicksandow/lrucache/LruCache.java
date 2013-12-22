/*
 * Copyright Nick Sandow 2013.
 */
package com.nicksandow.lrucache;

import java.util.Map;
import java.util.HashMap;

/**
 * A fixed size LRU (least recently used) cache.  When deciding to evict an 
 * item from the cache, the key that has least recently been written or read is evicted.
 * <p/>
 * The implementation consists of 2 data structures - a hash map that stores
 * the mapping from key to value, and a list that orders keys by recency
 * of access (with the key most recently accessed on the head of the list.)
 * <p/>
 * This class is not thread safe.
 */
public class LruCache<K, V>
{
    private Map<K, Entry<K, V>> nameToValueMap = new HashMap<K, Entry<K, V>>();

    // private final int capacity;
    private int currentSize;

    private Entry<K, V> mruHead;
    private Entry<K, V> mruTail;

    private Entry[] entries;

    private Stats stats;
    
    public LruCache(int capacity)
    {
        entries = new Entry[capacity]; // We have a dummy node on the front.

        mruHead = null;
        mruTail = null;

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

            // Retrieve the value from the backing data source.
            // Integer food = store.getFood(name);
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
     * This puts the entry {@code e} at the head of the MRU list.
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
            if (e == mruHead)
            {
                // It's already at the head, so do nothing.
            }
            else
            {
                // This could probably be simpler here.
                
                if (e == mruTail)
                {
                    // Set the new tail
                    entries[e.prev].next = -1;
                    mruTail = entries[e.prev];
                }
                else
                {
                    // It's somewhere in the middle.  Remove it.
                    entries[e.prev].next = e.next;
                    entries[e.next].prev = e.prev;
                }

                // Put it at the head of the list.
                e.prev = -1;
                e.next = mruHead.pos;
                mruHead.prev = e.pos;
                mruHead = e;
            }
        }
    }

    // Returns the slot of the entry that was evicted (and is now free).
    private Entry evict()
    {
        System.out.println("evicting item: " + mruTail);

        // Could implement dirty flag checking and only write if need be here, but just write for
        // now.
        // store.writeFood(mruTail.key, mruTail.value);

        nameToValueMap.remove(mruTail.key);

        return mruTail;
    }

    // Presumes there is space.  This is only used to fill the cache initially.
    private void addNewEntry(K key, V food)
    {
        Entry newNode = null;

        if (currentSize == 0)
        {
            newNode = entries[0] = new Entry(key, food, currentSize, -1, -1);
            mruHead = mruTail = entries[0];
        }
        else
        {
            // Add it to end then adjust.
            newNode = entries[currentSize] = new Entry(key, food, currentSize, -1, mruTail.pos);
            mruTail = entries[currentSize];
        }

        ++currentSize;
        updateLru(newNode);

        nameToValueMap.put(key, newNode);
    }

    private void setCacheEntry(Entry slot, K name, V food)
    {
        slot.key = name;
        slot.value = food;

        nameToValueMap.put(name, slot);
    }
    
    /**
     * Return the number of entries in the cache.  This will be an integer
     * in the range [0, 
     * @return 
     */
    public int getCurrentSize()
    {
        return currentSize;
    }
    
    public Stats getStats()
    {
        // Return a copy of our statistics object.
        return new Stats(stats);
    }
    
    public void printStats()
    {
        System.out.println("cache stats:");

        System.out.println("capacity: " + entries.length);
        System.out.println("current size: " + currentSize);

        System.out.println("MRU list:");
        int nextSlot = mruHead.pos;

        while (nextSlot != -1)
        {
            System.out.print("(" + entries[nextSlot].key + ", " + entries[nextSlot].value + ") -> ");

            nextSlot = entries[nextSlot].next;
        }
    }    

    private static class Entry<K, V>
    {
        Entry(K name, V food, int pos, int next, int prev)
        {
            this.key = name;
            this.value = food;
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
            return "name: " + key + " food: " + value + " pos: " + pos + " next: " + next + " prev: " + prev;
        }
    }

    /**
     * Stores basic cache statistics such as number of hits and misses, etc.
     * 
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

