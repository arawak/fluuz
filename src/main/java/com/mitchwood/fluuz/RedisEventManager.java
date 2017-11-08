package com.mitchwood.fluuz;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * Implements {@link EventManager} using Redis PUB/SUB.
 */
@Slf4j
public class RedisEventManager implements EventManager, Closeable {

    String channelName = "fluuz-eviction";
    String separator = "~~";

    ExecutorService executor = Executors.newFixedThreadPool(4);

    JedisPool pool;
    Jedis subscriberJedis;

    final JedisPubSub listener;

    Map<String, FluuzCache> cacheMap = new ConcurrentHashMap<String, FluuzCache>();

    public RedisEventManager(String host) {
        this(host, 6379);
    }

    public RedisEventManager(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        pool = new JedisPool(poolConfig, host, port, 0);
        subscriberJedis = pool.getResource();

        listener = new JedisPubSub() {

            @Override
            public void onMessage(String channel, String message) {
                log.error("received message '{}' on channel {}", message, channel);
                if (!channelName.equals(channel) || !StringUtils.hasText(message) || !message.contains(separator)) {
                    return;
                }

                String[] parts = StringUtils.split(message, separator);
                String cacheName = parts[0];

                String key = message.substring(cacheName.length() + separator.length());
                doEvict(cacheName, key);
            }

            @Override
            public void onPMessage(String pattern, String channel, String message) {
                log.error("received message '{}' on channel {} with pattern {}", message, channel, pattern);
            }
        };

        Runnable r = new Runnable() {
            public void run() {
                subscriberJedis.subscribe(listener, channelName);
            }
        };

        executor.execute(r);
    }

    public void close() throws IOException {
        // this might not be enough
        executor.shutdown();
        pool.close();
    }

    protected void doEvict(String cacheName) {
        FluuzCache cache = cacheMap.get(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    protected void doEvict(String cacheName, String key) {
        FluuzCache cache = cacheMap.get(cacheName);
        if (cache != null) {
            Cache proxy = cache.getProxy();
            proxy.evict(key);
            log.debug("evicted key '{}' from cache {}", key, cacheName);
        }
    }

    /**
     * Sends out an eviction notice for all mappings in the named cache.
     */
    public void evict(String name) {
        System.out.println("publishing an evict for " + name);
        String message = name + separator;
        pool.getResource().publish(channelName, message);
    }

    /**
     * Sends out an eviction notice the the given name and key.
     */
    public void evict(String name, Object key) {
        String message = name + separator + key.toString();
        pool.getResource().publish(channelName, message);
        log.debug("sent eviction notice: {}", message);
    }

    public void register(FluuzCache fluuzCache) {
        log.debug("registering cache {}", fluuzCache.getName());
        FluuzCache previous = cacheMap.put(fluuzCache.getName(), fluuzCache);
        if (previous != null) {
            log.warn("replacing previous instance of cache '{}'", fluuzCache.getName());
        }
    }

    /**
     * Override the default channel name. Only override if you know
     * what you're doing.
     *
     * @param channelName
     */
    public void setChannelName(String channelName) {
        Assert.hasText(channelName, "channel name cannot be empty");
        this.channelName = channelName;
    }

    /**
     * Override the default separator. Only override if you know
     * what you're doing.
     *      *
     * @param separator
     */
    public void setSeparator(String separator) {
        Assert.hasText(separator, "separator cannot be empty");
        this.separator = separator;
    }

}
