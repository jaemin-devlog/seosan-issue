package org.likelionhsu.backend.flask;

import org.likelionhsu.backend.flask.dto.request.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.response.SummarizeResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class FlaskSummarizeClient {

    private final WebClient webClient;

    // ★ 생성자 파라미터에 Qualifier 명시
    public FlaskSummarizeClient(@Qualifier("flaskWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public String summarize(String input) {
        SummarizeResponse res = webClient.post()
                .uri("/summarize")
                .bodyValue(new SummarizeRequest(input))
                .retrieve()
                .bodyToMono(SummarizeResponse.class)
                .timeout(Duration.ofSeconds(305))
                .block();
        return res == null ? null : res.summary();
    }
}
