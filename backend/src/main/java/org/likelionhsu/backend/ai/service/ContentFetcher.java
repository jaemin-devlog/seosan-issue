package org.likelionhsu.backend.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.likelionhsu.backend.ai.dto.ArticleText;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContentFetcher {

    /** 필요하면 여기 true 로 두고 테스트 (DEBUG 로그를 많이 뿌립니다) */
    private static final boolean VERBOSE = true;

    public ArticleText fetch(String url) {
        try {
            Connection conn = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com/")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .timeout(15000)
                    .method(Connection.Method.GET);

            long t0 = System.currentTimeMillis();
            Connection.Response res = conn.execute();
            long ms = System.currentTimeMillis() - t0;

            int status = res.statusCode();
            String ctype = res.contentType();
            int rawLen = res.bodyAsBytes() != null ? res.bodyAsBytes().length : -1;

            if (VERBOSE) {
                log.info("[FETCH] {} -> status={} ct={} bytes={} {}ms", url, status, ctype, rawLen, ms);
            }

            Document doc = res.parse();
            String title = safe(doc.title());

            // 1) 알려진 셀렉터들 시도 + 각 길이 로깅
            String text = selectBestKnownWithDebug(doc);

            // 2) 그래도 짧으면 가장 긴 텍스트 블록(간이 readability)
            if (isShort(text)) {
                String largest = pickLargestTextBlock(doc);
                if (VERBOSE) log.debug("[FETCH] {} largestBlock len={}", url, largest.length());
                if (largest.length() > text.length()) text = largest;
            }

            // 3) 마지막 보조: body 전체
            if (isShort(text) && doc.body() != null) {
                String bodyAll = doc.body().text();
                if (VERBOSE) log.debug("[FETCH] {} bodyAll len={}", url, bodyAll.length());
                if (bodyAll.length() > text.length()) text = bodyAll;
            }

            text = clean(text);

            if (VERBOSE) {
                log.info("[FETCH] {} final len={} sample=\"{}\"",
                        url, text.length(), sample(text, 120));
            }

            return new ArticleText(url, title, text, null);
        } catch (Exception e) {
            if (VERBOSE) log.warn("[FETCH] {} EXCEPTION: {}", url, e.toString());
            return null;
        }
    }

    private String selectBestKnownWithDebug(Document doc) {
        String best = "";
        int bestLen = 0;

        // 네이버/언론사 커버 셀렉터들
        String[] selectors = new String[]{
                // 네이버 뉴스(신규/모바일/구형)
                "#newsct_article", "#dic_area", "#newsEndContents", "#articleBodyContents",
                // 자주 보이는 언론사 컨테이너들
                "article", "#articeBody", ".article_body", ".articleBody", ".article-body",
                ".article-view", ".news_end", "#article", "#contents", ".news_body_area",
                ".text_body", ".news_content", ".view_cont", "#news-view", "#CmAdContent",
                ".content-article", ".art_text", "#textBody", ".cont_area"
        };

        for (String sel : selectors) {
            var el = doc.selectFirst(sel);
            int len = 0;
            String t = "";
            if (el != null) {
                t = safe(el.text());
                len = t.length();
            }
            log.debug("[FETCH][SEL] {} -> len={}", sel, len);
            if (len > bestLen) {
                bestLen = len;
                best = t;
            }
        }
        return best;
    }

    private String pickLargestTextBlock(Document doc) {
        String best = "";
        int bestLen = 0;
        for (org.jsoup.nodes.Element el : doc.select("article, div, section")) {
            String id = el.id().toLowerCase();
            String cls = el.className().toLowerCase();
            if (id.contains("comment") || id.contains("footer") || id.contains("head") || id.contains("nav")) continue;
            if (cls.contains("comment") || cls.contains("footer") || cls.contains("head") || cls.contains("nav")) continue;

            String t = safe(el.text());
            int len = t.length();
            if (len > bestLen) {
                bestLen = len;
                best = t;
            }
        }
        return best;
    }

    private boolean isShort(String s) {
        return s == null || s.trim().length() < 80; // 완화 기준
    }

    private String clean(String t) {
        if (t == null) return "";
        String x = t.replace("\u200B", " ")
                .replace("\u00A0", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
        // 흔한 저작권/광고 꼬리 제거(있으면)
        x = x.replaceAll("무단\\s*전재\\s*및\\s*재배포\\s*금지.*$", "");
        return x;
    }

    private String sample(String s, int n) {
        if (s == null) return "";
        if (s.length() <= n) return s;
        return s.substring(0, n) + "…";
    }

    private String safe(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }
}
