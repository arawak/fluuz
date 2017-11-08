package com.mitchwood.fluuz;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

public class FluuzCacheManager implements CacheManager {

    final CacheManager proxy;
    final EventManager eventManager;
    final ConcurrentHashMap<String, FluuzCache> cacheMap = new ConcurrentHashMap<String, FluuzCache>();

    /**
     * Construct a dynamic {@link FluuzCacheManager} backed by a {@link ConcurrentMapCacheManager}, 
     * lazily creating cache instances as they are being requested.
     */
    public FluuzCacheManager(EventManager eventManager) {
        this.eventManager = eventManager;
        proxy = new ConcurrentMapCacheManager();
    }

    /**
     * Construct a static {@link FluuzCacheManager}, managing caches for the
     * specified cache names only.
     */
    public FluuzCacheManager(EventManager eventManager, CacheManager cacheManager) {
        this.eventManager = eventManager;
        this.proxy = cacheManager;
    }

    public Cache getCache(String name) {
        FluuzCache fluuzCache = cacheMap.get(name);
        if (fluuzCache != null) {
            return fluuzCache;
        }
        Cache cache = proxy.getCache(name);

        if (cache != null) {
            fluuzCache = new FluuzCache(eventManager, cache);
            cacheMap.put(name, fluuzCache);
            eventManager.register(fluuzCache);
        }

        return fluuzCache;
    }

    public Collection<String> getCacheNames() {
        return proxy.getCacheNames();
    }

}
