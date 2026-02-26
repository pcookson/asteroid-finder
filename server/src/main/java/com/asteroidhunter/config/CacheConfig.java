package com.asteroidhunter.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${app.cache.neosTodayTtl:PT1H}") Duration neosTodayTtl,
            @Value("${app.cache.neosTodayMaxSize:10}") long neosTodayMaxSize) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("neosToday");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(neosTodayTtl)
                .maximumSize(neosTodayMaxSize)
                .recordStats());
        return cacheManager;
    }
}
