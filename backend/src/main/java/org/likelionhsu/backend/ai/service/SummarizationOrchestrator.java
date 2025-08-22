package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.dto.AiSearchDetailedResponse;
import org.likelionhsu.backend.ai.dto.AiSearchResponse;
import org.likelionhsu.backend.ai.dto.PerDocSummary;
import org.likelionhsu.backend.ai.filter.SourceDomainPolicy;
import org.likelionhsu.backend.ai.prompt.PromptTemplates;
import org.likelionhsu.backend.flask.FlaskSummarizeClient;
import org.likelionhsu.backend.naversearch.NaverSearchItemDto;
import org.likelionhsu.backend.naversearch.service.NaverSearchService;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.repository.PostRepository;
import org.likelionhsu.backend.post.repository.PostSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 단일 경로(Orchestrator 중심):
 *  검색 → 내부(게시글) + 외부(네이버 뉴스) 링크 수집 → 크롤링 → per-doc 요약 → reduce(TLDR)
 *  - 외부는 네이버 'news'만 사용 (blog/cafe 미포함)
 *  - sys/usr 분리해 Flask 호출
 *  - sanitizeResponse + postClean(JDK8) 후처리 적용
 */
@Service
@RequiredArgsConstructor
public class SummarizationOrchestrator {

    private final PostRepository postRepository;
    private final NaverSearchService naverSearchService;
    private final ContentFetcher contentFetcher;
    private final PerDocSummarizer perDocSummarizer;
    private final FlaskSummarizeClient flask;
    private final PromptTemplates prompts;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /* ========================= 리스트 요약 (/api/v1/ai-search) ========================= */
    public AiSearchResponse summarize(String query, int maxExternal) {
        final String q = StringUtils.defaultString(query).trim();
        final int n = normalize(maxExternal);

        // 내부
        List<Post> internalPosts = findInternalPosts(q, 5);

        // 외부(네이버 뉴스)
        List<NaverSearchItemDto> raw = Optional.ofNullable(naverSearchService.search("news", q, n))
                .orElseGet(Collections::emptyList);

        // (originallink, link) 쌍
        List<String[]> extPairs = raw.stream()
                .map(it -> new String[]{ normalizeLink(it.getOriginallink()), normalizeLink(it.getLink()) })
                .map(arr -> { // 둘 다 비면 버림
                    if (isBlank(arr[0]) && isBlank(arr[1])) return null;
                    return arr;
                })
                .filter(Objects::nonNull)
                // 너무 공격적인 차단을 피하기 위해 우선 허용 도메인 먼저 keep
                .filter(arr -> allowNewsDomain(arr[0]) || allowNewsDomain(arr[1]) || allowByPolicy(arr))
                .distinct()
                .limit(Math.max(5, n))
                .collect(Collectors.toList());

        // 디버그(임시): 어디서 비는지 확인
        System.out.printf("[AI-SEARCH] q=%s raw=%d extPairs=%d%n", q, raw.size(), extPairs.size());

        // 외부 본문 크롤링 (originallink → 실패 시 link, 반대로도 시도)
        // 본문 크롤링 전/후 로깅 (임시)
        int ok = 0, fail = 0;
        List<Doc> externalDocs = new ArrayList<>();
        for (String[] pair : extPairs) {
            Doc doc = fetchBest(pair[0], pair[1]);
            if (doc != null) { externalDocs.add(doc); ok++; }
            else { fail++; System.out.printf("[AI-SEARCH] fetchFail: a=%s b=%s%n", pair[0], pair[1]); }
        }
        System.out.printf("[AI-SEARCH] externalDocs ok=%d fail=%d%n", ok, fail);

        // 내부 → Doc
        List<Doc> internalDocs = mapInternal(internalPosts);

        // 합치기
        List<Doc> docs = dedupeByUrl(merge(internalDocs, externalDocs));

        // 디버그(필요 시 주석 해제)
        // System.out.printf("[AI-SEARCH] q=%s internal=%d raw=%d extPairs=%d externalDocs=%d allDocs=%d%n",
        //        q, internalPosts.size(), raw.size(), extPairs.size(), externalDocs.size(), docs.size());

        if (docs.isEmpty()) {
            return new AiSearchResponse("관련 소스를 찾지 못했습니다.", List.of());
        }

        // per-doc 요약
        List<PerDocSummary> items = new ArrayList<>();
        for (Doc d : docs) {
            PerDocSummary one = perDocSummarizer.summarizeOne(
                    d.url, d.title, d.sourceType, d.publishedAt, d.body
            );
            if (one != null) items.add(one);
        }

        // reduce 입력: summary 없으면 content 폴백
        String joinedFacts = items.stream()
                .map(it -> {
                    String s = it.summary();
                    if (s == null || s.trim().isEmpty()) s = abbrev(safe(it.content()), 400);
                    return s;
                })
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));

        // reduce
        String sys = prompts.reduceSystem();
        String usr = prompts.reduceUser(joinedFacts);
        String tldr;
        try {
            tldr = Optional.ofNullable(flask.summarize(sys, usr).block())
                    .orElse("")
                    .trim();
        } catch (Exception e) {
            tldr = "";
        }
        tldr = postClean(sanitizeResponse(tldr));

        List<String> sources = docs.stream()
                .map(d -> d.url)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.<String>comparingInt(u -> SourceDomainPolicy.isLikelyTrusted(u) ? 0 : 1)
                        .thenComparing(Comparator.naturalOrder()))
                .limit(8)
                .collect(Collectors.toList());

        if (StringUtils.isBlank(tldr)) tldr = "핵심 요약을 생성하지 못했습니다.";
        return new AiSearchResponse(tldr, sources);
    }

    /* ========================= 상세 요약 (/api/v1/ai-search/detail) ========================= */
    public AiSearchDetailedResponse summarizeDetailed(String query, int maxExternal) {
        final String q = StringUtils.defaultString(query).trim();
        final int n = normalize(maxExternal);

        List<Post> internalPosts = findInternalPosts(q, 5);
        List<NaverSearchItemDto> raw = Optional.ofNullable(naverSearchService.search("news", q, n))
                .orElseGet(Collections::emptyList);

        List<String[]> extPairs = raw.stream()
                .map(it -> new String[]{ normalizeLink(it.getOriginallink()), normalizeLink(it.getLink()) })
                .map(arr -> isBlank(arr[0]) && isBlank(arr[1]) ? null : arr)
                .filter(Objects::nonNull)
                .filter(this::allowByPolicyOrNews)
                .distinct()
                .limit(Math.max(5, n))
                .collect(Collectors.toList());
        // 디버그(임시): 어디서 비는지 확인
        System.out.printf("[AI-SEARCH] q=%s raw=%d extPairs=%d%n", q, raw.size(), extPairs.size());


        // 본문 크롤링 전/후 로깅 (임시)
        int ok = 0, fail = 0;
        List<Doc> externalDocs = new ArrayList<>();
        for (String[] pair : extPairs) {
            Doc doc = fetchBest(pair[0], pair[1]);
            if (doc != null) { externalDocs.add(doc); ok++; }
            else { fail++; System.out.printf("[AI-SEARCH] fetchFail: a=%s b=%s%n", pair[0], pair[1]); }
        }
        System.out.printf("[AI-SEARCH] externalDocs ok=%d fail=%d%n", ok, fail);

        List<Doc> internalDocs = mapInternal(internalPosts);
        List<Doc> docs = dedupeByUrl(merge(internalDocs, externalDocs));

        if (docs.isEmpty()) {
            return new AiSearchDetailedResponse("관련 소스를 찾지 못했습니다.", List.of(), List.of());
        }

        List<PerDocSummary> items = new ArrayList<>();
        for (Doc d : docs) {
            PerDocSummary one = perDocSummarizer.summarizeOne(
                    d.url, d.title, d.sourceType, d.publishedAt, d.body
            );
            if (one != null) items.add(one);
        }

        String joinedFacts = items.stream()
                .map(it -> {
                    String s = it.summary();
                    if (s == null || s.trim().isEmpty()) s = abbrev(safe(it.content()), 400);
                    return s;
                })
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));

        String sys = prompts.reduceSystem();
        String usr = prompts.reduceUser(joinedFacts);
        String tldr;
        try {
            tldr = Optional.ofNullable(flask.summarize(sys, usr).block())
                    .orElse("")
                    .trim();
        } catch (Exception e) {
            tldr = "";
        }
        tldr = postClean(sanitizeResponse(tldr));

        List<String> sources = docs.stream()
                .map(d -> d.url)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.<String>comparingInt(u -> SourceDomainPolicy.isLikelyTrusted(u) ? 0 : 1)
                        .thenComparing(Comparator.naturalOrder()))
                .limit(8)
                .collect(Collectors.toList());

        if (StringUtils.isBlank(tldr)) tldr = "핵심 요약을 생성하지 못했습니다.";
        return new AiSearchDetailedResponse(tldr, items, sources);
    }

    /* ========================= 프리뷰: Controller에서 호출 ========================= */
    public String buildPromptPreview(String query, int maxExternal) {
        final String q = StringUtils.defaultString(query).trim();
        final int n = normalize(maxExternal);

        List<Post> internalPosts = findInternalPosts(q, 3);

        List<String> extLinks = Optional.ofNullable(naverSearchService.search("news", q, n))
                .orElseGet(Collections::emptyList).stream()
                .map(NaverSearchItemDto::getLink)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(u -> !SourceDomainPolicy.isBlocked(u))
                .distinct()
                .limit(Math.max(3, n))
                .collect(Collectors.toList());

        StringBuilder data = new StringBuilder();
        if (!internalPosts.isEmpty()) {
            data.append("## 내부 공지\n");
            for (Post p : internalPosts) {
                data.append("- ").append(safe(p.getTitle()))
                        .append(": ").append(abbrev(safe(p.getContent()), 300)).append("\n");
            }
        }
        if (!extLinks.isEmpty()) {
            data.append("## 외부(네이버 뉴스)\n");
            for (String url : extLinks) {
                try {
                    var page = contentFetcher.fetch(url);
                    if (page == null || StringUtils.isBlank(page.text())) continue;
                    data.append("- ").append(safe(page.title()))
                            .append(": ").append(abbrev(safe(page.text()), 400)).append("\n");
                } catch (Exception ignore) {}
            }
        }

        String sys = prompts.reduceSystem();
        return sys + "\n[DATA]\n" + data;
    }

    /* ========================= helpers ========================= */

    private List<Post> findInternalPosts(String q, int limit) {
        var page = postRepository.findAll(
                PostSpecification.containsKeyword(q),
                PageRequest.of(0, Math.max(1, limit),
                        Sort.by(Sort.Order.desc("pubDate"), Sort.Order.desc("crawledAt")))
        );
        return page.getContent();
    }

    private static class Doc {
        final String url;
        final String title;
        final String sourceType;
        final String publishedAt;
        final String body;

        Doc(String url, String title, String sourceType, String publishedAt, String body) {
            this.url = url;
            this.title = title;
            this.sourceType = sourceType;
            this.publishedAt = publishedAt;
            this.body = body;
        }
    }

    private List<Doc> merge(List<Doc> a, List<Doc> b) {
        List<Doc> out = new ArrayList<>();
        if (a != null) out.addAll(a);
        if (b != null) out.addAll(b);
        return out.stream()
                .filter(Objects::nonNull)
                .filter(d -> StringUtils.isNotBlank(d.body))
                .collect(Collectors.toList());
    }

    private List<Doc> dedupeByUrl(List<Doc> docs) {
        if (docs == null || docs.isEmpty()) return Collections.emptyList();
        Map<String, Doc> byUrl = new LinkedHashMap<>();
        for (Doc d : docs) {
            String key = d.url == null ? UUID.randomUUID().toString() : d.url;
            byUrl.putIfAbsent(key, d);
        }
        return new ArrayList<>(byUrl.values());
    }

    /* ---------- 외부 fetch 유틸 ---------- */

    private Doc fetchBest(String maybeOriginal, String maybeNaverLink) {
        // 우선 순위: originallink → 실패 시 link → (반대로도 한 번 더)
        Doc d = tryFetch(maybeOriginal);
        if (d != null) return d;
        d = tryFetch(maybeNaverLink);
        if (d != null) return d;
        // 교차로 한 번 더
        d = tryFetch(maybeOriginal);
        if (d != null) return d;
        return tryFetch(maybeNaverLink);
    }

    // tryFetch: 길이 기준 완화
    private Doc tryFetch(String url) {
        if (isBlank(url)) return null;
        try {
            var page = contentFetcher.fetch(url);
            if (page == null) {
                System.out.printf("[AI-SEARCH] tryFetch null: %s%n", url);
                return null;
            }

            // ✅ ArticleText에서 text/content/body 어떤 필드여도 다 잡아내기
            String body = extractBody(page);

            System.out.printf("[AI-SEARCH] tryFetch len=%d url=%s sample=\"%s\" (pageClass=%s)%n",
                    body.length(), url,
                    (body.length() > 80 ? body.substring(0, 80) + "…" : body),
                    page.getClass().getName());

            if (body.length() < 80) return null; // 기준 완화
            return new Doc(url, safe(page.title()), "EXTERNAL", null, body);
        } catch (Exception e) {
            System.out.printf("[AI-SEARCH] tryFetch EX: %s -> %s%n", url, e.toString());
            return null;
        }
    }

    private boolean allowNewsDomain(String u) {
        if (isBlank(u)) return false;
        String host = hostOf(u);
        if (host == null) return false;
        return host.endsWith("news.naver.com") || host.endsWith("n.news.naver.com");
    }

    private boolean allowByPolicy(String[] pair) {
        String a = pair[0], b = pair[1];
        return (a != null && !SourceDomainPolicy.isBlocked(a))
                || (b != null && !SourceDomainPolicy.isBlocked(b));
    }

    private boolean allowByPolicyOrNews(String[] pair) {
        return allowNewsDomain(pair[0]) || allowNewsDomain(pair[1]) || allowByPolicy(pair);
    }

    private String hostOf(String u) {
        try {
            return new java.net.URI(u).getHost();
        } catch (Exception e) { return null; }
    }

    private List<Doc> mapInternal(List<Post> internalPosts) {
        List<Doc> list = new ArrayList<>();
        for (Post p : internalPosts) {
            String title = safe(p.getTitle());
            String body  = safe(p.getContent());
            if (StringUtils.isBlank(body) && StringUtils.isNotBlank(p.getLink())) {
                try {
                    var page = contentFetcher.fetch(p.getLink());
                    if (page != null && StringUtils.isNotBlank(page.text())) {
                        body = safe(page.text());
                    }
                } catch (Exception ignore) {}
            }
            if (StringUtils.isBlank(body)) body = title;

            String publishedAt = null;
            try {
                String s = safe(p.getPubDate());
                publishedAt = s.isEmpty() ? null : s;
            } catch (Exception ignore) {}

            list.add(new Doc(
                    Optional.ofNullable(p.getLink()).orElse("internal://post/" + p.getId()),
                    title, "INTERNAL", publishedAt, body
            ));
        }
        return list;
    }

    private int normalize(Integer in) {
        if (in == null || in <= 0) return 3;
        return Math.min(in, 10);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String abbrev(String s, int len) {
        if (s == null) return "";
        if (s.length() <= len) return s;
        return s.substring(0, Math.max(0, len - 1)).trim() + "…";
    }

    /** 허용 문자만 유지 (이모지/태그/제어문자 제거) */
    private String sanitizeResponse(String input) {
        if (input == null) return "";
        // 필요시 허용 문자 확장 가능
        return input.replaceAll("[^가-힣a-zA-Z0-9 .,!]", "").trim();
    }

    /** JDK8 호환 반복 축약 & 문장 정리 */
    private String postClean(String s) {
        if (s == null) return "";
        String x = s.replaceAll("\\s+", " ").trim();

        // 같은 글자(공백 포함) 3회 이상 반복 -> 1회
        x = x.replaceAll("([가-힣A-Za-z0-9])(?:\\s*\\1){2,}", "$1");

        // 짧은 토큰(1~3자) 반복 3회 이상 -> 1회
        x = x.replaceAll("(?:(\\b[가-힣A-Za-z0-9]{1,3}\\b))(?:\\s+\\1){2,}", "$1");

        // 한 글자 토큰만 연속 3개 이상 → 마지막 한 글자만
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?:\\b[가-힣A-Za-z]\\b\\s*){3,}");
        java.util.regex.Matcher m = p.matcher(x);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String grp = m.group();
            java.util.regex.Matcher m2 = java.util.regex.Pattern
                    .compile("\\b([가-힣A-Za-z])\\b")
                    .matcher(grp);
            String last = null;
            while (m2.find()) last = m2.group(1);
            m.appendReplacement(sb, last == null ? "" : java.util.regex.Matcher.quoteReplacement(last));
        }
        m.appendTail(sb);
        x = sb.toString();

        // 문장 끝 보정
        if (!x.matches(".*[.!?…]$")) x = x + ".";

        // 끝의 군더더기 구두점 제거
        x = x.replaceAll("[,;:·]+$", "").trim();

        return x;
    }

    private static boolean isBlank(String s){ return s == null || s.trim().isEmpty(); }

    private static String normalizeLink(String u) {
        if (u == null) return null;
        String v = u.replace("&amp;", "&").replace("\u200B", "").trim();
        if (v.startsWith("//")) v = "https:" + v;
        return v;
    }

    /** ArticleText가 record/POJO 어떤 형태여도 본문을 최대한 안전하게 뽑는다. */
    private String extractBody(Object page) {
        if (page == null) return "";
        // 1) 가장 흔한 메서드 이름들 순회
        String[] candidates = new String[]{
                "text", "getText",          // record/POJO 혼용
                "content", "getContent",
                "body", "getBody"
        };
        for (String m : candidates) {
            try {
                var method = page.getClass().getMethod(m);
                Object val = method.invoke(page);
                if (val instanceof CharSequence cs) {
                    String s = safe(cs.toString());
                    if (!s.isEmpty()) return s;
                }
            } catch (NoSuchMethodException ignore) {
            } catch (Exception e) {
                // 예상치 못한 예외는 로깅만
                System.out.printf("[AI-SEARCH] extractBody %s EX: %s%n", m, e.toString());
            }
        }
        // 2) 필드 직접 접근(레거시 호환)
        String[] fields = new String[]{"text", "content", "body"};
        for (String f : fields) {
            try {
                var field = page.getClass().getDeclaredField(f);
                field.setAccessible(true);
                Object val = field.get(page);
                if (val instanceof CharSequence cs) {
                    String s = safe(cs.toString());
                    if (!s.isEmpty()) return s;
                }
            } catch (NoSuchFieldException ignore) {
            } catch (Exception e) {
                System.out.printf("[AI-SEARCH] extractBody field %s EX: %s%n", f, e.toString());
            }
        }
        return "";
    }
}
