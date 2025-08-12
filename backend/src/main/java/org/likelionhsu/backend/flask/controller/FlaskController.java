package org.likelionhsu.backend.flask.controller;

import org.likelionhsu.backend.flask.dto.request.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.response.SummarizeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/flask")
public class FlaskController {

    private final RestTemplate restTemplate;

    @Value("${crawler.api.url}")
    private String crawlerApiUrl;

    public FlaskController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @GetMapping("/crawl_all")
    public ResponseEntity<?> crawlAll() {
        String url = crawlerApiUrl + "/crawl_all";
        return restTemplate.getForEntity(url, Object.class);
    }

    @PostMapping("/summarize")
    public ResponseEntity<SummarizeResponse> summarize(@RequestBody SummarizeRequest request) {
        String url = crawlerApiUrl + "/summarize";
        return restTemplate.postForEntity(url, request, SummarizeResponse.class);
    }

    @GetMapping("/crawl_popular_terms")
    public ResponseEntity<?> getPopularTerms() {
        String url = crawlerApiUrl + "/crawl_popular_terms";
        return restTemplate.getForEntity(url, Object.class);
    }

    @GetMapping("/content_stats")
    public ResponseEntity<?> getContentStats() {
        String url = crawlerApiUrl + "/content_stats";
        return restTemplate.getForEntity(url, Object.class);
    }
}