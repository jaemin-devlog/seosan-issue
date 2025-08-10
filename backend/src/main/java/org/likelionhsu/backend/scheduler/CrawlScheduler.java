package org.likelionhsu.backend.scheduler;

import org.likelionhsu.backend.flask.FlaskController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlScheduler {

    private final FlaskController flaskController;

    public CrawlScheduler(FlaskController flaskController) {
        this.flaskController = flaskController;
    }

    @Scheduled(fixedRate = 10800000) // 3시간마다 실행 (밀리초 단위)
    public void scheduleCrawlAll() {
        flaskController.crawlAll();
    }
}