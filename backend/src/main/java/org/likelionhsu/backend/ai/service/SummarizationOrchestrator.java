package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.dto.AiSearchDetailedResponse;
import org.likelionhsu.backend.ai.dto.AiSearchResponse;
import org.likelionhsu.backend.ai.dto.PerDocSummary;
import org.likelionhsu.backend.ai.filter.ContentQualityGate;
import org.likelionhsu.backend.ai.filter.SourceDomainPolicy;
import org.likelionhsu.backend.flask.FlaskSummarizeClient;
import org.likelionhsu.backend.naversearch.NaverSearchItemDto;
import org.likelionhsu.backend.naversearch.service.NaverSearchService;
import org.likelionhsu.backend.post.repository.PostRepository;
import org.likelionhsu.backend.post.repository.PostSpecification;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 프롬프트 자체가 요약되는 문제를 방지:
 * - [DATA] 섹션에 '기사 스니펫'만 넣는다 (명령/규칙 텍스트는 모델 입력으로만 쓰이고 요약 대상에서 제외)
 * - 데이터가 없으면 LLM 호출하지 않고 즉시 "관련 소스를 찾지 못했습니다." 반환
 * - 블로그/카페 차단 + 품질 게이트 + 후처리 유지
 */
@Service
@RequiredArgsConstructor
public class SummarizationOrchestrator {

    private final PostRepository postRepository;
    private final NaverSearchService naverSearchService;
    private final ContentFetcher contentFetcher;
    private final PerDocSummarizer perDocSummarizer;
    private final FlaskSummarizeClient flask;
    private static final int MIN_INTERNAL_CONTENT_LEN = 100;

    /* ========================= 리스트 요약 (/api/v1/ai-search) ========================= */

    @Cacheable(cacheNames = "summary", cacheManager = "redisCacheManager",
            key = "'oneshot::' + #query + '::' + #maxExternal",
            unless = "#result == null || #result.getSummary() == null || #result.getSummary().isBlank()")
    public AiSearchResponse summarize(String query, int maxExternal) {

        // 0) 내부 공지(최신 5)
        var pageReq = PageRequest.of(0, 5, Sort.Direction.DESC, "crawledAt");
        var internalPosts = postRepository.findAll(
                PostSpecification.containsKeyword(query), pageReq
        ).getContent();

        // 1) 외부 후보 수집 (news/blog/cafearticle) — 시그니처: (type, query, display)
        List<NaverSearchItemDto> news  = naverSearchService.search("news",        query, maxExternal);
        List<NaverSearchItemDto> blogs = naverSearchService.search("blog",        query, maxExternal);
        List<NaverSearchItemDto> cafes = naverSearchService.search("cafearticle", query, Math.max(1, maxExternal - 1));

        // 2) 링크 모으기 + 도메인 필터 + 중복 제거
        List<String> extLinks = Stream.of(news, blogs, cafes)
                .filter(Objects::nonNull)
                .flatMap(list -> list.stream().map(NaverSearchItemDto::getLink))
                .filter(Objects::nonNull)
                .filter(u -> !SourceDomainPolicy.isBlocked(u)) // 블로그/카페 컷
                .distinct()
                .limit(Math.max(5, maxExternal))
                .collect(Collectors.toList());

        // 3) 본문 fetch → 품질 게이트
        var pages = extLinks.stream()
                .map(contentFetcher::fetch)
                .filter(Objects::nonNull)
                .map(a -> new PageLite(a.url(), safe(a.title()), safe(a.text())))
                .filter(p -> ContentQualityGate.pass(p.title, p.body, Set.of())) // 빈 Set 전달
                .limit(Math.max(3, maxExternal))
                .toList();

        // ⚠️ 데이터가 전무하면 LLM 호출 금지
        boolean hasInternal = !internalPosts.isEmpty();
        boolean hasExternal = !pages.isEmpty();
        if (!hasInternal && !hasExternal) {
            return new AiSearchResponse("관련 소스를 찾지 못했습니다.", List.of());
        }

        // 4) 프롬프트 구성 — [SYSTEM] + [DATA] (명령/규칙은 모델 유도용, 요약 대상은 [DATA]만)
        String system = """
목표: 서로 다른 출처를 합쳐 한 문장으로 핵심만 요약한다. 두번째 문장은 상황에 따라 추가 설명을 붙인다.
길이: 35~80자 사이를 권장(너무 짧게 끊지 말 것).
규칙:
- 따옴표·괄호·광고·출처 문구·“라고 전했다/밝혔다/전한” 등 인용 표현 제거
- ‘누가/어디/무엇(현상·조치)/언제’를 우선, 사실만
- 기사 제목을 그대로 쓰지 말 것
- 한 문장에서 두 문장 출력
""";

        String data = buildDataBlock(internalPosts, pages);

        String prompt = system + "\n[DATA]\n" + data;

        // 5) 요약 호출 + 후처리
        String summary = flask.summarize(prompt);
        summary = postProcessOneLine(summary);

        // 6) sources 구성 — 내부 링크 + 외부 링크 (신뢰도 우선 정렬, 중복 제거)
        List<String> sources = Stream.concat(
                        internalPosts.stream().map(p -> p.getLink()).filter(Objects::nonNull),
                        extLinks.stream())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(Comparator.<String>comparingInt(u -> SourceDomainPolicy.isLikelyTrusted(u) ? 0 : 1)
                        .thenComparing(Comparator.naturalOrder()))
                .limit(5)
                .toList();

        if (summary == null || summary.isBlank()) summary = "핵심 요약을 생성하지 못했습니다.";
        return new AiSearchResponse(summary, sources);
    }

    private String buildDataBlock(List<?> internalPosts, List<PageLite> pages) {
        StringBuilder sb = new StringBuilder();
        if (internalPosts != null && !internalPosts.isEmpty()) {
            sb.append("## 내부 공지\n");
            internalPosts.forEach(p -> {
                // p.getTitle(), p.getContent(), p.getLink() 사용 가능
                try {
                    var title = safe((String) p.getClass().getMethod("getTitle").invoke(p));
                    var content = safe((String) p.getClass().getMethod("getContent").invoke(p));
                    sb.append("- ").append(title).append(": ").append(abbrev(content, 500)).append("\n");
                } catch (Exception ignore) {}
            });
        }
        if (pages != null && !pages.isEmpty()) {
            sb.append("## 외부 자료\n");
            pages.forEach(p -> sb.append("- ").append(p.title).append(": ")
                    .append(abbrev(p.body, 700)).append("\n"));
        }
        return sb.toString();
    }

    /* ========================= 상세 요약 (/api/v1/ai-search/detail) ========================= */

    @Cacheable(cacheNames = "summary", cacheManager = "redisCacheManager",
            key = "'mapreduce::' + #query + '::' + #maxExternal",
            unless = "#result == null || #result.tldr() == null || #result.tldr().isBlank()")
    public AiSearchDetailedResponse summarizeDetailed(String query, int maxExternal) {

        // 1) 내부 공지
        var pageReq = PageRequest.of(0, 5, Sort.Direction.DESC, "crawledAt");
        var internalPosts = postRepository.findAll(
                PostSpecification.containsKeyword(query), pageReq
        ).getContent();

        // 2) 외부 후보 수집 — (type, query, display)
        List<NaverSearchItemDto> news  = naverSearchService.search("news",        query, maxExternal);
        List<NaverSearchItemDto> blogs = naverSearchService.search("blog",        query, maxExternal);
        List<NaverSearchItemDto> cafes = naverSearchService.search("cafearticle", query, Math.max(1, maxExternal - 1));

        List<String> extLinks = Stream.of(news, blogs, cafes)
                .filter(Objects::nonNull)
                .flatMap(list -> list.stream().map(NaverSearchItemDto::getLink))
                .filter(Objects::nonNull)
                .filter(u -> !SourceDomainPolicy.isBlocked(u))
                .distinct()
                .limit(Math.max(5, maxExternal))
                .collect(Collectors.toList());

        var items = new ArrayList<PerDocSummary>();

        // 내부 per-doc 요약 (본문 비었을 때 폴백 포함) — ✅ 요약 유무와 관계없이 items에 추가
        for (var p : internalPosts) {
            String t = safe(p.getTitle());
            String c = safe(p.getContent());

            String internalText = StringUtils.isNotBlank(c) ? c : t; // 1차 폴백: 제목
            // 2차 폴백: 링크에서 본문 크롤링 (링크가 있고 본문이 여전히 비었을 때)
            if (StringUtils.isBlank(internalText) && p.getLink() != null) {
                var page = contentFetcher.fetch(p.getLink());
                if (page != null && StringUtils.isNotBlank(page.text())) {
                    internalText = page.text();
                }
            }
            // 요약 요청 시 url도 폴백 (sources 표시에 도움)
            String url = (p.getLink() != null)
                    ? p.getLink()
                    : ("internal://post/" + p.getId());

            var one = perDocSummarizer.summarizeOne(url, t, "internal", null, internalText);
            if (one != null) items.add(one); // ❗️요약이 비어도 추가 (content/link만 있어도 포함)
        }

        // 외부 per-doc 요약 — ✅ 품질 게이트/길이 체크 없이 무조건 summarizeOne에 태움
        for (String url : extLinks) {
            var page = contentFetcher.fetch(url);
            String title = (page != null) ? safe(page.title()) : "";
            String body  = (page != null) ? safe(page.text())  : "";

            var one = perDocSummarizer.summarizeOne(url, title, "external", null, body);
            if (one != null) items.add(one); // ❗️요약이 비어도 추가
        }

        if (items.isEmpty()) {
            return new AiSearchDetailedResponse("관련 소스를 찾지 못했습니다.", List.of(), List.of());
        }

        // Reduce 프롬프트(데이터만)
        StringBuilder reduceData = new StringBuilder();
        items.forEach(it -> reduceData.append("- ").append(abbrev(safe(it.summary()), 500)).append("\n"));

        String reduceSystem = """
역할: 한국어 뉴스 TLDR 작성기
목표: 아래 요약 조각들을 통합해 1문장(최대 28자)으로 핵심만 요약한다.
규칙:
- 인용·광고·출처 문구 제거, 사실만
- 서로 상충하면 '내부' 출처를 우선
- 한 문장만 출력
""";

        String tldr = flask.summarize(reduceSystem + "\n[DATA]\n" + reduceData);
        tldr = postProcessOneLine(tldr);

        // sources: 내부 링크 + 외부 링크(신뢰도 우선)
        List<String> sources = Stream.concat(
                        internalPosts.stream().map(p -> p.getLink()).filter(Objects::nonNull),
                        extLinks.stream())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(Comparator.<String>comparingInt(u -> SourceDomainPolicy.isLikelyTrusted(u) ? 0 : 1)
                        .thenComparing(Comparator.naturalOrder()))
                .limit(8)
                .toList();

        return new AiSearchDetailedResponse(
                (tldr == null || tldr.isBlank()) ? "핵심 요약을 생성하지 못했습니다." : tldr,
                items,
                sources
        );
    }

    /* ========================= Helpers ========================= */

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String abbrev(String s, int len) {
        if (s == null) return "";
        if (s.length() <= len) return s;
        return s.substring(0, Math.max(0, len - 1)).trim() + "…";
    }

    private static final Pattern MULTI_QUOTES = Pattern.compile("[\"'“”‘’]{2,}");
    private static final Pattern MULTI_SPACES = Pattern.compile("\\s{2,}");
    private static final Pattern QUOTE_REPORTED = Pattern.compile("(라고\\s*(전한|전했다|밝혔다|전한다)[^.]*\\.?)+");

    /** 한 줄 요약 후처리: 따옴표/공백/“라고 전한…” 제거 */
    private static String postProcessOneLine(String s) {
        if (s == null) return "";
        String out = MULTI_QUOTES.matcher(s).replaceAll("'");
        out = MULTI_SPACES.matcher(out).replaceAll(" ");
        out = QUOTE_REPORTED.matcher(out).replaceAll("");
        out = out.replaceAll("^[-•\\s]+", "").trim();

        // ❶ 여러 문장이면 첫 번째 ‘완결된’ 문장만 남기기 (마침표/물음표/느낌표 기준)
        String tmp = out + " ";
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(.*?[.!?])\\s").matcher(tmp);
        if (m.find()) out = m.group(1).trim();

        // ❷ 문장부호 없이 끝나면 마침표 보정
        if (!out.matches(".*[.!?…]$")) {
            out = out + ".";
        }

        // ❸ 끝에 어색한 구두점/조사만 남았으면 정리
        out = out.replaceAll("[,;:·]+$", "").trim();
        return out;
    }

    /** 가벼운 내부 표현 */
    private record PageLite(String url, String title, String body) {}

    /** 프롬프트 프리뷰(LLM 호출 없이 입력만 반환) — 리스트 요약용 디버그 */
    public String buildPromptPreview(String query, int maxExternal) {
        // 0) 내부 공지
        var pageReq = org.springframework.data.domain.PageRequest.of(0, 5,
                org.springframework.data.domain.Sort.Direction.DESC, "crawledAt");
        var internalPosts = postRepository.findAll(
                org.likelionhsu.backend.post.repository.PostSpecification.containsKeyword(query), pageReq
        ).getContent();

        // 1) 외부 후보 수집 — (type, query, display)
        var news  = naverSearchService.search("news",        query, maxExternal);
        var blogs = naverSearchService.search("blog",        query, maxExternal);
        var cafes = naverSearchService.search("cafearticle", query, Math.max(1, maxExternal - 1));

        // 2) 링크 모으기 + 도메인 필터 + 중복 제거
        java.util.List<String> extLinks = java.util.stream.Stream.of(news, blogs, cafes)
                .filter(java.util.Objects::nonNull)
                .flatMap(list -> list.stream().map(org.likelionhsu.backend.naversearch.NaverSearchItemDto::getLink))
                .filter(java.util.Objects::nonNull)
                .filter(u -> !org.likelionhsu.backend.ai.filter.SourceDomainPolicy.isBlocked(u))
                .distinct()
                .limit(Math.max(5, maxExternal))
                .toList();

        // 3) 본문 fetch → 품질 게이트 (프리뷰는 기존 로직 유지)
        var pages = extLinks.stream()
                .map(contentFetcher::fetch)
                .filter(Objects::nonNull)
                .map(a -> new PageLite(a.url(), safe(a.title()), safe(a.text())))
                .filter(p -> ContentQualityGate.pass(p.title, p.body, Set.of())) // 빈 Set 전달
                .limit(Math.max(3, maxExternal))
                .toList();

        // [DATA] 블록만 미리 보여주기
        String data = buildDataBlock(internalPosts, pages);

        String system = """
목표: 서로 다른 출처를 합쳐 최대 7문장 내외로 요약하라. 내용이 다를 경우 문장을 나눠서 요약하라.
길이: 35~80자 사이를 권장(너무 짧게 끊지 말 것).
규칙:
- 따옴표·괄호·광고·출처 문구·“라고 전했다/밝혔다/전한” 등 인용 표현 제거
- ‘누가/어디/무엇(현상·조치)/언제’를 우선, 사실만
- 기사 제목을 그대로 쓰지 말 것
- 한 문장에서 두 문장 출력
""";

        return system + "\n[DATA]\n" + data;
    }
}
