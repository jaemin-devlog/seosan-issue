package org.likelionhsu.backend.flask;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.flask.dto.request.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.response.SummarizeResponse;
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
        if (res.getBody() == null) {
            return null; // 응답 본문이 없을 경우 null 반환
        }
        return res.getBody().summary(); // 응답 본문에서 summary 필드 반환
    }
}
