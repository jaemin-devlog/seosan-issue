package org.likelionhsu.backend.ai.util;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ArticleCleaner {

    private ArticleCleaner() {}

    // 반복 따옴표/공백 제거 패턴
    private static final Pattern MULTI_QUOTES = Pattern.compile("[\"'“”‘’]{2,}");
    private static final Pattern MULTI_SPACES = Pattern.compile("\\s{2,}");

    // 전역 불용문구 (네이버 래퍼/안내문 등)
    private static final List<Pattern> GLOBAL_NOISE = List.of(
            Pattern.compile("기사원문"),
            Pattern.compile("성별\\s*말하기\\s*속도"),
            Pattern.compile("이동\\s*통신망.*?데이터\\s*통화료.*?부과될\\s*수\\s*있습니다\\.?"),
            Pattern.compile("무단전재\\s*및\\s*재배포\\s*금지"),
            Pattern.compile("ⓒ\\s*연합뉴스"),
            Pattern.compile("\\[\\s*속보\\s*\\]")
    );

    /** canonical(or og:url) 우선 */
    public static String canonicalUrl(Document doc, String fallback) {
        String canon = Optional.ofNullable(doc.selectFirst("link[rel=canonical]"))
                .map(e -> e.attr("href")).orElse(null);
        if (canon == null || canon.isBlank()) {
            canon = Optional.ofNullable(doc.selectFirst("meta[property=og:url]"))
                    .map(e -> e.attr("content")).orElse(null);
        }
        return (canon != null && !canon.isBlank()) ? canon : fallback;
    }

    /** 매체별 본문 선택자 우선 추출 */
    public static String extractBody(String sourceHost, Document doc) {
        if (sourceHost == null) sourceHost = "";

        // 1) 네이버 뉴스
        if (sourceHost.contains("n.news.naver.com") || sourceHost.contains("news.naver.com")) {
            Element body = doc.selectFirst("#newsct_article");
            if (body != null) return body.text();
        }

        // 2) 네이버 블로그 (모바일/구형)
        if (sourceHost.contains("m.blog.naver.com") || sourceHost.contains("blog.naver.com")) {
            Element se = doc.selectFirst(".se-main-container");
            if (se != null && se.text().length() > 50) return se.text();
            Element old1 = doc.selectFirst("#postViewArea");
            if (old1 != null && old1.text().length() > 50) return old1.text();
            Element old2 = doc.selectFirst("#post-view");
            if (old2 != null && old2.text().length() > 50) return old2.text();
        }

        // 3) 네이버 카페 (모바일/데스크탑)
        if (sourceHost.contains("m.cafe.naver.com") || sourceHost.contains("cafe.naver.com")) {
            Element se = doc.selectFirst(".se-main-container");
            if (se != null && se.text().length() > 50) return se.text();
            for (String sel : List.of("#tbody", "#content-area", ".article_viewer", "#article", ".ContentRenderer")) {
                Element e = doc.selectFirst(sel);
                if (e != null && e.text().length() > 50) return e.text();
            }
        }

        // 4) 일반 매체 공통
        for (String sel : List.of("article", ".article", ".art_cont", "#articleBody",
                ".news_body", ".content", "#content", ".article-body", ".post-content", ".entry-content")) {
            Element e = doc.selectFirst(sel);
            if (e != null && e.text().length() > 200) return e.text();
        }
        return doc.body() != null ? doc.body().text() : "";
    }

    /** 제목 정리 */
    public static String normalizeTitle(String title) {
        if (title == null) return null;
        String t = title.replaceAll("\\[속보\\]\\s*", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return t;
    }

    /** 본문 정리 */
    public static String clean(String raw) {
        if (raw == null) return null;
        String text = raw;
        for (Pattern p : GLOBAL_NOISE) text = p.matcher(text).replaceAll(" ");
        text = MULTI_QUOTES.matcher(text).replaceAll("'");
        text = MULTI_SPACES.matcher(text).replaceAll(" ").trim();
        return text;
    }


}


