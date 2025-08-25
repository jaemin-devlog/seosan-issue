package org.likelionhsu.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")   // 모든 Origin 허용
                        .allowedMethods("*")          // 모든 메서드 허용
                        .allowedHeaders("*")          // 모든 헤더 허용
                        .allowCredentials(false)      // 세션/쿠키 필요 없으니 false
                        .maxAge(86400);               // 24시간 캐싱
            }
        };
    }
}
