package org.likelionhsu.backend.flask;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.flask.dto.SummarizeRequest;
import org.likelionhsu.backend.flask.dto.SummarizeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flask")
@RequiredArgsConstructor
public class FlaskController {

    private final FlaskService flaskService;

    @GetMapping("/crawl_all")
    public ResponseEntity<?> crawlAll() {
        return flaskService.crawlAll();
    }

    @PostMapping("/summarize")
    public ResponseEntity<SummarizeResponse> summarize(@RequestBody SummarizeRequest request) {
        return flaskService.summarize(request);
    }

    @GetMapping("/crawl_popular_terms")
    public ResponseEntity<?> getPopularTerms() {
        return flaskService.getPopularTerms();
    }
}
