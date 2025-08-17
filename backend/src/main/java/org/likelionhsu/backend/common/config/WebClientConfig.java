package org.likelionhsu.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder, @Value("${crawler.api.url:http://crawler:5001}") String crawlerBaseUrl) {
        return builder.baseUrl(crawlerBaseUrl).build();
    }
}