package org.likelionhsu.backend.ai.service;

import net.dankito.readability4j.Readability4J;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.likelionhsu.backend.ai.dto.ArticleText;
import org.likelionhsu.backend.ai.filter.SourceDomainPolicy;
import org.likelionhsu.backend.ai.util.ArticleCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ContentFetcher {
    private static final Logger log = LoggerFactory.getLogger(ContentFetcher.class);

    private static final String UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Safari/537.36";
    private static final String UA_MOBILE  = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Mobile Safari/537.36";

    @Cacheable(cacheNames = "content", cacheManager = "redisCacheManager", key = "#url",
            unless = "#result == null || #result.text() == null || #result.text().isBlank()")
    public ArticleText fetch(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent(UA_DESKTOP).timeout(10_000).get();
            String canon = ArticleCleaner.canonicalUrl(doc, url);
            if (!canon.equals(url)) {
                try {
                    doc = Jsoup.connect(canon)
                            .userAgent(UA_DESKTOP).referrer(url).timeout(10_000).get();
                    url = canon;
                } catch (Exception ignore) {}
            }

            String host = SourceDomainPolicy.host(url);
            // 💡 네이버 블로그: desktop은 iframe(mainFrame) → m.blog.naver.com 로 재요청
            if (host != null && host.endsWith("blog.naver.com")) {
                Element iframe = doc.selectFirst("iframe#mainFrame");
                if (iframe != null) {
                    String src = iframe.attr("src");
                    String abs = src.startsWith("http") ? src : "https://blog.naver.com" + (src.startsWith("/") ? src : ("/" + src));
                    log.debug("[Fetcher] Naver blog iframe -> {}", abs);
                    doc = Jsoup.connect(abs).userAgent(UA_MOBILE).referrer(url).timeout(10_000).get();
                    url = abs;
                    host = SourceDomainPolicy.host(url);
                }
            }

            // 💡 네이버 카페: og:url이 m.cafe.naver.com/ArticleRead... 인 경우 많음 → 그걸로 리로드
            if (host != null && host.endsWith("cafe.naver.com")) {
                String og = Optional.ofNullable(doc.selectFirst("meta[property=og:url]"))
                        .map(e -> e.attr("content")).orElse(null);
                if (og != null && og.contains("m.cafe.naver.com")) {
                    log.debug("[Fetcher] Naver cafe og:url -> {}", og);
                    doc = Jsoup.connect(og).userAgent(UA_MOBILE).referrer(url).timeout(10_000).get();
                    url = og;
                    host = SourceDomainPolicy.host(url);
                }
            }

            String title = ArticleCleaner.normalizeTitle(doc.title());
            String body  = ArticleCleaner.extractBody(host, doc);
            if (body == null || body.length() < 80) {
                // Readability 보강
                var readability = new Readability4J(url, doc.outerHtml());
                var article = readability.parse();
                if (article != null) {
                    if (title == null || title.isBlank()) title = article.getTitle();
                    if (body == null || body.length() < 80) body = article.getTextContent();
                }
            }

            title = ArticleCleaner.normalizeTitle(title);
            body  = ArticleCleaner.clean(body);

            if (body == null || body.isBlank()) {
                log.info("[Fetcher] Empty body after extraction: {}", url);
                return null;
            }
            return new ArticleText(url, title, null, body);

        } catch (Exception e) {
            log.warn("[Fetcher] Fail: {} -> {}", url, e.toString());
            return null;
        }
    }
}
