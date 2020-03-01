package com.mitchwood.fluuz;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import redis.embedded.RedisServer;

public class RedisEventManagerTest {

    static RedisServer redisServer = new RedisServer();
    static RedisEventManager eventManager;

    @BeforeAll
    public static void beforeAll() {
        redisServer.start();
        eventManager = new RedisEventManager("localhost", 6379);
    }
    
    @Test
    public void testPut() throws Exception {
        Cache proxy = new ConcurrentMapCache("foo");
        proxy.put("bar", "value");
        proxy.put("baz", "value");
        FluuzCache fluuzCache = new FluuzCache(eventManager, proxy);
        eventManager.register(fluuzCache);
        Thread.sleep(2000);

        ValueWrapper vw = proxy.get("bar");
        assertThat(vw, notNullValue());
        assertThat(vw.get(), equalTo("value"));
        
        eventManager.evict("foo", "bar");

        Thread.sleep(2000);

        vw = proxy.get("bar");
        assertThat(vw, nullValue());
    }

    @AfterAll
    public static void cleanup() {
        try {
            eventManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        redisServer.stop();
    }
}
