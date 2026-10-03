package com.tang.tianji.config;

import java.time.Duration;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.*;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
  @Bean
  @Profile("!test & !local")
  public org.springframework.cache.CacheManager cacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory)
        .cacheDefaults(
            RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(name -> "tianji-ai-academy::" + name + "::")
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues())
        .build();
  }

  @Bean
  @Profile({"test", "local"})
  public org.springframework.cache.CacheManager testCacheManager() {
    return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
      private void warn() {
        org.slf4j.LoggerFactory.getLogger(CacheConfig.class)
            .warn("category_cache_unavailable: using database");
      }

      public void handleCacheGetError(RuntimeException e, Cache c, Object k) {
        warn();
      }

      public void handleCachePutError(RuntimeException e, Cache c, Object k, Object v) {
        warn();
      }

      public void handleCacheEvictError(RuntimeException e, Cache c, Object k) {
        warn();
      }

      public void handleCacheClearError(RuntimeException e, Cache c) {
        warn();
      }
    };
  }
}
