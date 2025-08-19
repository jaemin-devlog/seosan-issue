package org.likelionhsu.backend.flask;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.flask.dto.request.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.response.SummarizeResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class FlaskSummarizeClient {
    @Qualifier("flaskWebClient")
    private final WebClient webClient;

    public String summarize(String input) {
        SummarizeResponse res = webClient.post()
                .uri("/summarize")
                .bodyValue(new SummarizeRequest(input))
                .retrieve()
                .bodyToMono(SummarizeResponse.class)
                .timeout(Duration.ofSeconds(305))   // ★ 백엔드 앱 단에서 최종 가드
                .block();
        return res == null ? null : res.summary();
    }
}
