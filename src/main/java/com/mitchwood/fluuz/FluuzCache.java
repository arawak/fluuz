package com.mitchwood.fluuz;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Wraps a proxy {@link Cache} instance and registers it
 * with an {@link EventManager}
 */
public class FluuzCache implements Cache {

    Cache proxy;
    EventManager eventManager;

    public FluuzCache(EventManager eventManager, Cache proxy) {
        Assert.notNull(proxy, "null proxy");
        this.proxy = proxy;
        this.eventManager = eventManager;
        eventManager.register(this);
    }

    public String getName() {
        return proxy.getName();
    }

    public Object getNativeCache() {
        return proxy.getNativeCache();
    }

    public ValueWrapper get(Object key) {
        return proxy.get(key);
    }

    public <T> T get(Object key, Class<T> type) {
        return proxy.get(key, type);
    }

    public <T> T get(Object key, Callable<T> valueLoader) {
        return proxy.get(key, valueLoader);
    }

    public void put(Object key, Object value) {
        proxy.put(key, value);
    }

    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper vw = proxy.putIfAbsent(key, value);
        return vw;
    }

    public void evict(Object key) {
        proxy.evict(key);
        eventManager.evict(proxy.getName(), key);
    }

    /**
     * This will publish a "clear cache" message for the named cache.
     *
     * @param cacheName
     */
    public void publishClear(String cacheName) {
        if (! StringUtils.isEmpty(cacheName)) {
            eventManager.evict(cacheName);
        }
    }

    public void clear() {
        proxy.clear();
    }

    /**
     * Return the underlying {@link Cache}.
     *
     * @return
     */
    public Cache getProxy() {
        return proxy;
    }

}
