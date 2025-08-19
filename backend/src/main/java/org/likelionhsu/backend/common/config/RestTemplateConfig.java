package org.likelionhsu.backend.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfig {

    @Bean
    public WebClient flaskWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)      // connect 10s
                .responseTimeout(Duration.ofSeconds(300))                  // ★ 전체 응답 300s
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(305, TimeUnit.SECONDS))  // 소켓 idle read
                        .addHandlerLast(new WriteTimeoutHandler(305, TimeUnit.SECONDS)) // 소켓 idle write
                );

        return builder
                .baseUrl("http://crawler:5001")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                        .build())
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Accept-Encoding", "gzip")
                .build();
    }
}
