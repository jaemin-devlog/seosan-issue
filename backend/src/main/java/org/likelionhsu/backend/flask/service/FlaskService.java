
// src/main/java/.../flask/service/FlaskService.java
package org.likelionhsu.backend.flask.service;

import org.likelionhsu.backend.common.config.FlaskClientConfig;
import org.likelionhsu.backend.flask.dto.request.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.response.SummarizeResponse;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class FlaskService {
    private static final Logger log = LoggerFactory.getLogger(FlaskService.class);

    private final RestTemplate restTemplate;
    private final FlaskClientConfig cfg;

    public FlaskService(RestTemplate restTemplate, FlaskClientConfig cfg) {
        this.restTemplate = restTemplate;
        this.cfg = cfg;
    }

    public ResponseEntity<?> crawlAll(Integer pages) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(cfg.getUrl() + "/crawl_all");

        if (pages != null) {
            builder.queryParam("pages", pages);
        }

        String url = builder.build(true).toUriString();
        log.info("GET {}", url);
        return restTemplate.getForEntity(url, Object.class);
    }

    public ResponseEntity<?> contentStats() {
        var url = cfg.getUrl() + "/content_stats";
        log.info("GET {}", url);
        return restTemplate.getForEntity(url, Object.class);
    }

    public ResponseEntity<?> popularTerms() {
        var url = cfg.getUrl() + "/crawl_popular_terms";
        log.info("GET {}", url);
        return restTemplate.getForEntity(url, Object.class);
    }

    public ResponseEntity<SummarizeResponse> summarize(SummarizeRequest request) {
        var url = cfg.getUrl() + "/summarize";
        log.info("POST {}", url);
        return restTemplate.postForEntity(url, request, SummarizeResponse.class);
    }

    
}
