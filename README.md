# fluuz

A Spring CacheManager with distributed evictions.

Distributed caches solve the problem of consistency at the expense of 
performance. In general, distributed caches rely on serialization of objects,
and synchronization of cache nodes.

An in-memory cache such as Guava or caffeine provides the best performance, at
the expense of consistency. For high-read low-write situations it is tempting 
to use an in-memory cache, however there is the risk that caches can become 
stale and out of sync.

This project aims to provide in-memory caches with no stale data. Whenever a
key/value is forcibly evicted, this eviction is broadcast to other nodes
who then evict the key. Local evictions due to policy are not broadcast, as 
they do not represent a change of the state of data.


## Limitations

A system using this strategy needs to be written with it in mind. Specifically,
whenever data that might be cached is altered at the source (datastore), an
eviction notice must be published for the affected keys.

## Design

Fluuz provides a `CacheManager` implementation that wraps another 
`CacheManager` and accepts an `EventManager` implementation. The `EventManager`
keeps track of forced evictions, notifying peers of local forced evictions, and
propagating evictions from peers' notifications. In this way a cache might 
miss, but will not be out of date.

The underlying `CacheManager`s do not need to be configured identically or even
be of the same implementation type between nodes. One node might use a 
`CaffeineCacheManager` with a LRU eviction policy and another might use a
'EhCacheCacheManager' with a LFU policy and write to disk. All peers will 
however need to use the same `EventManager` implementation, configured 
consistently.

## Implementation

`RedisEventManager` is provided as an `EventManager` implementation that uses
Redis PUB/SUB for communication between nodes. It requires a Redis server (or 
cluster) host and port in its constructor.
 

