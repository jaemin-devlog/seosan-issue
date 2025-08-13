package org.likelionhsu.backend.ai.service;

import net.dankito.readability4j.Readability4J; // ✅ Readability4J 사용
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class ContentFetcher {

    // 다른 클래스에서 import 하려면: import org.likelionhsu.backend.ai.service.ContentFetcher.ArticleText;
    public record ArticleText(String url, String title, String byline, String text) {}

    @Cacheable(cacheNames = "content", cacheManager = "redisCacheManager", key = "#url",
            unless = "#result == null || #result.text() == null || #result.text().isBlank()")
    public ArticleText fetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; SeosanIssueBot/1.0)")
                    .timeout(8_000)
                    .get();

            var readability = new Readability4J(url, doc.outerHtml());
            var article = readability.parse();

            return new ArticleText(url, article.getTitle(), article.getByline(), article.getTextContent());
        } catch (Exception e) {
            return null; // 실패 시 캐시 X
        }
    }
}
