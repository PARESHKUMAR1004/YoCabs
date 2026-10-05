package com.yocabs.api.shared.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * A single in-process cache for anything cheap to compute again but expensive or slow to fetch
 * from elsewhere (a routing provider call, for instance). One Railway instance today, so an
 * in-memory cache is enough; a multi-instance deployment would need a shared store (Redis)
 * instead.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("routes");
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .maximumSize(5_000)
        );
        return manager;
    }
}
