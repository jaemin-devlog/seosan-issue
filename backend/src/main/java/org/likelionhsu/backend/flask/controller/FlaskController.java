// src/main/java/.../flask/controller/FlaskController.java
package org.likelionhsu.backend.flask.controller;

import org.likelionhsu.backend.flask.dto.request.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.response.SummarizeResponse;
import org.likelionhsu.backend.flask.service.FlaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flask")
public class FlaskController {

    private final FlaskService flaskService;

    public FlaskController(FlaskService flaskService) {
        this.flaskService = flaskService;
    }

    @GetMapping("/crawl_all")
    public ResponseEntity<?> crawlAll(
            @RequestParam(required = false, defaultValue = "2") Integer pages
    ) {
        return flaskService.crawlAll(pages);
    }

    @GetMapping("/content_stats")
    public ResponseEntity<?> getContentStats() {
        return flaskService.contentStats();
    }

    @GetMapping("/crawl_popular_terms")
    public ResponseEntity<?> getPopularTerms() {
        return flaskService.popularTerms();
    }

    @PostMapping("/summarize")
    public ResponseEntity<SummarizeResponse> summarize(@RequestBody SummarizeRequest request) {
        return flaskService.summarize(request);
    }

    
}
