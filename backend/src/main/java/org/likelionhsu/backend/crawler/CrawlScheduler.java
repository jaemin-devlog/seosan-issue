package org.likelionhsu.backend.crawler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class CrawlScheduler {

    private static final Logger log = LoggerFactory.getLogger(CrawlScheduler.class);
    private final WebClient webClient;

    // 3시간마다 전체 카테고리 크롤링
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void crawlAll() {
        try {
            String res = webClient.get()
                    .uri("/crawl_all")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("[crawl_all] result={}", res);
        } catch (Exception e) {
            log.warn("[crawl_all] failed: {}", e.getMessage());
        }
    }
}
