lrucache
========

A simple, fixed-size LRU cache in Java.

This code is somewhat tested and seems to work but I've not used it in anything real.

The cache is implemented with 2 data structures - a hash map providing a mapping from keys to
values, and a list sorted by recency of access.  By keeping the list sorted by access time the
operation of evicting the least recently used item is straightforward.

The key and value types are type parameters (the LruCache.java is a template class) and consequently
can be any object type.

There is also currently some cache hit/miss counters that can be inspected.

An example of using the cache to store the results of a lengthy computation is also included in the
`example` package.


