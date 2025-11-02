package com.johnbeo.johnbeo.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            // CryptoDataService caches
            "coins.market",
            "coins.detail", 
            "coins.marketChart",
            "coins.simplePrice",
            "coins.marketByIds",
            // Other caches
            "trendingPosts",
            "notificationCount",
            "popularTags"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1_000));
        return cacheManager;
    }

    @Bean
    public Cache<String, Boolean> postViewCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(10_000)
            .build();
    }
}
