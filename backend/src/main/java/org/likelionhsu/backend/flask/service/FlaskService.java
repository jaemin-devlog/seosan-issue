package org.likelionhsu.backend.flask.service;

import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.flask.dto.request.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.response.SummarizeResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class FlaskService {

    private final WebClient flask;

    // ★ 생성자 파라미터에 Qualifier 명시
    public FlaskService(@Qualifier("flaskWebClient") WebClient flask) {
        this.flask = flask;
    }

    public ResponseEntity<?> crawlAll(Integer pages) {
        var body = flask.get()
                .uri(uriBuilder -> uriBuilder.path("/crawl_all")
                        .queryParamIfPresent("pages", pages == null
                                ? java.util.Optional.empty()
                                : java.util.Optional.of(pages))
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(305))
                .block();
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<?> popularTerms() {
        var body = flask.get()
                .uri("/crawl_popular_terms")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(305))
                .block();
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<?> contentStats() {
        var body = flask.get()
                .uri("/content_stats")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(305))
                .block();
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<SummarizeResponse> summarize(SummarizeRequest req) {
        var res = flask.post()
                .uri("/summarize")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(SummarizeResponse.class)
                .timeout(Duration.ofSeconds(305))
                .block();
        return ResponseEntity.ok(res);
    }
}
