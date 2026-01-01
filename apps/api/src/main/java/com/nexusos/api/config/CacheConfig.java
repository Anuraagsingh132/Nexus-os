package com.nexusos.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("rate-limit-ws", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    public redis.clients.jedis.JedisPool jedisPool(org.springframework.boot.autoconfigure.data.redis.RedisProperties redisProperties) {
        return new redis.clients.jedis.JedisPool(redisProperties.getHost(), redisProperties.getPort());
    }
}
