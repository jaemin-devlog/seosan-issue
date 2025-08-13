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

    // discovery는 기존 CacheConfig의 caffeineCacheManager를 그대로 씁니다.
    // 여기서는 redisCacheManager 이름을 가진 "대체" Caffeine 캐시를 만듭니다.
    @Primary // ✅ 로컬에서도 기본 CacheManager는 이걸로
    @Bean(name = "redisCacheManager")
    public CacheManager fakeRedisCacheManager() {
        CaffeineCacheManager m = new CaffeineCacheManager("content", "summary");
        m.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(2))   // summary 기본 TTL
                .maximumSize(10_000));
        return m;
    }
}
