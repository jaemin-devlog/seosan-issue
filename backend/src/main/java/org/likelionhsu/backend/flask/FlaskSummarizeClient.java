package org.likelionhsu.backend.flask;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FlaskSummarizeClient {

    private final WebClient flaskWebClient;

    @Value("${ai.summarizer.temperature:0.2}")
    private double temperature;
    @Value("${ai.summarizer.top-p:0.3}")
    private double topP;
    @Value("${ai.summarizer.repetition-penalty:1.18}")
    private double repetitionPenalty;
    @Value("${ai.summarizer.max-tokens:220}")
    private int maxTokens;
    @Value("${ai.prompt.stop-sequences:}")
    private List<String> stopSequences;

    public Mono<String> summarize(String system, String user) {
        Map<String, Object> payload = Map.of(
                "system", system,
                "user", user,
                "generation", Map.of(
                        "temperature", temperature,
                        "top_p", topP,
                        "repetition_penalty", repetitionPenalty,
                        "max_tokens", maxTokens,
                        "stop", stopSequences
                )
        );

        return flaskWebClient.post()
                .uri("/summarize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }
}
