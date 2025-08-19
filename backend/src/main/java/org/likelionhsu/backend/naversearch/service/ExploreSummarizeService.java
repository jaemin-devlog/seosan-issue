package org.likelionhsu.backend.naversearch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.ai.filter.ContentQualityGate;
import org.likelionhsu.backend.ai.filter.SourceDomainPolicy;
import org.likelionhsu.backend.ai.service.ContentFetcher;
import org.likelionhsu.backend.ai.service.PerDocSummarizer;
import org.likelionhsu.backend.ai.util.ArticleCleaner;
import org.likelionhsu.backend.naversearch.dto.ExploreSummarizeDtos.ItemSummaryResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExploreSummarizeService {

    private final ContentFetcher contentFetcher;     // 이미 있음
    private final PerDocSummarizer perDocSummarizer; // 이미 있음

    /** 단건 URL 요약 (본문은 절대 반환하지 않음) */
    @Cacheable(cacheNames = "summary", cacheManager = "redisCacheManager",
            key = "'explore:url:' + #url",
            unless = "#result == null || #result.getSummary() == null || #result.getSummary().isBlank()")
    public ItemSummaryResponse summarizeUrl(String url) {
        if (url == null || SourceDomainPolicy.isHardBlocked(url)) return null;
        try {
            var page = contentFetcher.fetch(url);
            if (page == null || org.apache.commons.lang3.StringUtils.isBlank(page.text())) {
                log.info("[Explore] fetch failed or empty: {}", url);
                return null;
            }

            String title = ArticleCleaner.normalizeTitle(page.title());
            String body  = ArticleCleaner.clean(page.text());
            var terms = quickTermsFromUrlOrTitle(url, title);

            String host = SourceDomainPolicy.host(url);
            if (!ContentQualityGate.pass(title, body, terms, host)) {
                log.info("[Explore] quality gate fail: host={}, len={}", host, body.length());
                return null;
            }

            var one = perDocSummarizer.summarizeOne(
                    url, title, "external", extractPublishedAtStr(url), body
            );
            if (one == null || org.apache.commons.lang3.StringUtils.isBlank(one.summary())) {
                log.info("[Explore] per-doc summarize empty: {}", url);
                return null;
            }

            return org.likelionhsu.backend.naversearch.dto.ExploreSummarizeDtos.ItemSummaryResponse.builder()
                    .url(url)
                    .title(title)
                    .summary(one.summary())
                    .sourceType("external")
                    .publishedAt(null)
                    .build();

        } catch (Exception e) { // IOException 포함
            log.warn("[Explore] summarizeUrl failed: {}", url, e);
            return null;
        }
    }


    private static Set<String> quickTermsFromUrlOrTitle(String url, String title) {
        String seed = (title == null ? "" : title) + " " + (url == null ? "" : url);
        String[] toks = seed.toLowerCase().split("\\s+|/|\\?|&|=|-|_|\\.");
        Set<String> out = new LinkedHashSet<>();
        for (String t : toks) {
            t = t.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHangul}]", "");
            if (t.length() >= 2) out.add(t);
        }
        if (out.contains("서산")) { out.add("충남"); out.add("태안"); out.add("당진"); }
        if (out.stream().anyMatch(s -> s.contains("호우"))) { out.add("호우주의보"); out.add("호우경보"); out.add("특보"); }
        return out;
    }

    private static Instant extractPublishedAt(org.likelionhsu.backend.ai.dto.ArticleText at) {
        // ArticleText에 publishedAt이 없으면 null 반환 (2단계에서 파서 붙일 수 있음)
        try {
            var f = at.getClass().getDeclaredField("publishedAt");
            f.setAccessible(true);
            Object v = f.get(at);
            return (v instanceof Instant) ? (Instant) v : null;
        } catch (Exception ignore) { return null; }
    }

    private static String extractPublishedAtStr(String url) {
        if (url == null) return null;
        // URL 안에 2025-08-16 / 20250816 / 2025.08.16 같은 패턴이 있으면 추출
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(20\\d{2})[./-]?(\\d{1,2})[./-]?(\\d{1,2})")
                .matcher(url);
        if (!m.find()) return null;

        String y = m.group(1);
        String mm = m.group(2);
        String dd = m.group(3);

        // 두 자리 보정
        if (mm.length() == 1) mm = "0" + mm;
        if (dd.length() == 1) dd = "0" + dd;

        return y + "-" + mm + "-" + dd; // e.g., 2025-08-16
    }

}
