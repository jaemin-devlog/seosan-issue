package org.likelionhsu.backend.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class HttpClientsConfig {

    /** 요청/응답 타이밍 로깅 */
    private static ExchangeFilterFunction timing(String tag) {
        return (request, next) -> {
            long t0 = System.nanoTime();
            return next.exchange(request)
                    .doOnNext(res -> {
                        long ms = (System.nanoTime() - t0) / 1_000_000;
                        log.info("[{}] {} {} -> {} {}ms",
                                tag, request.method(), request.url(),
                                res.rawStatusCode(), ms);
                    })
                    .doOnError(err -> {
                        long ms = (System.nanoTime() - t0) / 1_000_000;
                        log.warn("[{}] {} {} -> ERR {}ms : {}",
                                tag, request.method(), request.url(), ms, err.toString());
                    });
        };
    }

    /** 큰 응답(최대 8MB) 허용 */
    private static ExchangeStrategies strategies(int mb) {
        return ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(mb * 1024 * 1024))
                .build();
    }

    /** Flask 모델 서버 (체인 300s) */
    @Bean("flaskWebClient")
    public WebClient flaskWebClient(
            WebClient.Builder builder,
            @Value("${crawler.api.url:http://crawler:5001}") String baseUrl
    ) {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000)
                .responseTimeout(Duration.ofSeconds(300))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(305, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(305, TimeUnit.SECONDS)));

        return builder.baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(strategies(8))
                .filter(timing("flask"))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Accept-Encoding", "identity")
                .build();
    }

    /** 외부 API (NAVER/KMA 등, 90s) */
    @Bean("externalWebClient")
    public WebClient externalWebClient(WebClient.Builder builder) {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(90));

        return builder
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(strategies(8))
                .filter(timing("ext"))
                .defaultHeader("Accept", "application/json")
//                .defaultHeader("Accept-Encoding", "identity")
                .build();
    }
}
