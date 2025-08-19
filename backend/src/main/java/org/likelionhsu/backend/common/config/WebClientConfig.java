package org.likelionhsu.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebClientConfig implements WebMvcConfigurer {

    @Bean
    public WebClient webClient(WebClient.Builder builder, @Value("${crawler.api.url:http://crawler:5001}") String crawlerBaseUrl) {
        return builder.baseUrl(crawlerBaseUrl).build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 설정을 적용합니다.
                .allowedOrigins("https://seosan-issue.web.app", "https://seosan-issue.firebaseapp.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 HTTP 메서드를 지정합니다.
                .allowedHeaders("*") // 모든 헤더를 허용합니다.
                .allowCredentials(true); // 쿠키 등 자격 증명 정보를 허용합니다.
    }
}