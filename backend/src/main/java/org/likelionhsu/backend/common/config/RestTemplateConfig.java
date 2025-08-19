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
        // 1) Netty 클라이언트 타임아웃/핸들러
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)     // connect 10s
                .responseTimeout(Duration.ofSeconds(120))                  // 전체 응답 120s
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(130, TimeUnit.SECONDS))   // 소켓 read idle
                        .addHandlerLast(new WriteTimeoutHandler(130, TimeUnit.SECONDS))  // 소켓 write idle
                );

        // 2) 큰 응답 대비(예: 4MB)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();

        return builder
                .baseUrl("http://crawler:5001")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeaders(h -> {
                    h.add("Accept", "application/json");
                    h.add("Accept-Encoding", "gzip"); // 압축 허용
                })
                .build();
    }
}
