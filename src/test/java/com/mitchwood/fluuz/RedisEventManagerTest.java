package com.mitchwood.fluuz;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.concurrent.ConcurrentMapCache;

public class RedisEventManagerTest {

    static RedisEventManager v;

    @BeforeClass
    public static void setup() {
        //v = new RedisEventManager("lchu-dvco-xdo1", 6379);
        v = new RedisEventManager("localhost", 6379);
    }

    @Test
    public void testPut() throws Exception {
        Cache proxy = new ConcurrentMapCache("foo");
        proxy.put("bar", "value");
        proxy.put("baz", "value");
        FluuzCache fluuzCache = new FluuzCache(v, proxy);
        v.register(fluuzCache);
        Thread.sleep(2000);

        v.evict("foo", "bar");

        Thread.sleep(2000);

        String value = null;
        ValueWrapper vw = proxy.get("bar");
        if (vw == null) {

        }
        System.out.println("->" + value);
    }

    @AfterClass
    public static void cleanup() {
        try {
            v.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
