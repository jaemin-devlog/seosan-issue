package org.likelionhsu.backend.flask;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.flask.dto.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.SummarizeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class FlaskSummarizeClient {
    private final RestTemplate restTemplate;
    @Value("${flask.server.url}")
    private String flaskServerUrl;

    public String summarize(String input) {
        String url = flaskServerUrl + "/summarize";
        ResponseEntity<SummarizeResponse> res = restTemplate.postForEntity(url, new SummarizeRequest(input), SummarizeResponse.class);
        return res.getBody() != null ? res.getBody().getSummary() : null;
    }
}
