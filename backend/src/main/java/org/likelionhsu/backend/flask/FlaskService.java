package org.likelionhsu.backend.flask;

import org.likelionhsu.backend.flask.dto.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.SummarizeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FlaskService {

    private static final Logger log = LoggerFactory.getLogger(FlaskService.class);
    private final RestTemplate restTemplate;

    @Value("${crawler.api.url}")
    private String flaskServerUrl;

    public FlaskService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<?> crawlAll() {
        String url = flaskServerUrl + "/crawl_all";
        log.info("FlaskService에서 크롤러 API 호출 시도: GET {}", url);
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            log.info("크롤러 API 호출 성공. 상태 코드: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("크롤러 API 호출 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body("크롤러 API 호출 실패: " + e.getMessage());
        }
    }

    public ResponseEntity<SummarizeResponse> summarize(SummarizeRequest request) {
        String url = flaskServerUrl + "/summarize";
        log.info("FlaskService에서 크롤러 API 호출 시도: POST {}", url);
        try {
            ResponseEntity<SummarizeResponse> response = restTemplate.postForEntity(url, request, SummarizeResponse.class);
            log.info("크롤러 API 호출 성공. 상태 코드: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("크롤러 API 호출 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    public ResponseEntity<?> getPopularTerms() {
        String url = flaskServerUrl + "/crawl_popular_terms";
        log.info("FlaskService에서 크롤러 API 호출 시도: GET {}", url);
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            log.info("크롤러 API 호출 성공. 상태 코드: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("크롤러 API 호출 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body("크롤러 API 호출 실패: " + e.getMessage());
        }
    }
}
