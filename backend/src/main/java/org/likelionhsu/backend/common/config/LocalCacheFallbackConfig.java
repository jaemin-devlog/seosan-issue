package org.likelionhsu.backend.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.*;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;

// 로컬 개발 전용: Redis 없이도 돌아가게, 같은 이름(redisCacheManager)으로 Caffeine 대체

@Configuration
@Profile("local")
public class LocalCacheFallbackConfig {

    @Primary
    @Bean(name = "redisCacheManager")
    public CacheManager fakeRedisCacheManager() {
        CaffeineCacheManager m = new CaffeineCacheManager("content", "summary", "perdoc"); // ✅ perdoc 포함
        m.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(2))
                .maximumSize(10_000));
        return m;
    }
}