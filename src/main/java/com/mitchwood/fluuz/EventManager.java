package com.mitchwood.fluuz;

public interface EventManager {

    void evict(String name, Object key);
    void evict(String name);

    /**
     * Lets the EventManager keep track of managed caches
     * @param fluuzCache
     */
    void register(FluuzCache fluuzCache);

}
