package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.dto.AiSearchDetailedResponse;
import org.likelionhsu.backend.ai.dto.AiSearchResponse;
import org.likelionhsu.backend.ai.dto.PerDocSummary;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummarizationOrchestrator {

    private final PostRepository postRepository;
    private final NaverSearchService naverSearchService;
    private final ContentFetcher contentFetcher;
    private final PerDocSummarizer perDocSummarizer; // ⬅️ 새로 추가될 서비스
    private final FlaskSummarizeClient flask;

    /** 기존의 간단 요약(one-shot): 하위 호환 유지 */
    @Cacheable(cacheNames = "summary", cacheManager = "redisCacheManager",
            key = "'oneshot::' + #query + '::' + #maxExternal",
            unless = "#result == null || #result.getSummary() == null || #result.getSummary().isBlank()")
    public AiSearchResponse summarize(String query, int maxExternal) {

        var pageReq = PageRequest.of(0, 5, Sort.Direction.DESC, "crawledAt");
        var internalPosts = postRepository.findAll(
                PostSpecification.containsKeyword(query), pageReq
        ).getContent();

        // 외부 링크 수집
        List<NaverSearchItemDto> news = naverSearchService.search("news", query, maxExternal);
        List<NaverSearchItemDto> blogs = naverSearchService.search("blog", query, maxExternal);
        List<NaverSearchItemDto> cafes = naverSearchService.search("cafearticle", query, Math.max(1, maxExternal - 1));

        List<String> externalLinks = new ArrayList<>();
        externalLinks.addAll(news.stream().map(NaverSearchItemDto::getLink).toList());
        externalLinks.addAll(blogs.stream().map(NaverSearchItemDto::getLink).toList());
        externalLinks.addAll(cafes.stream().map(NaverSearchItemDto::getLink).toList());

        // 본문 추출 (Top N)
        var pages = externalLinks.stream()
                .filter(Objects::nonNull).distinct().limit(maxExternal)
                .map(contentFetcher::fetch)
                .filter(Objects::nonNull)
                .toList();

        // 프롬프트 조립
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 동일 주제에 대한 여러 출처의 자료다. 모든 출처를 종합하여 사실 중심의 한 문단 요약을 작성하라. 서로 상충하면 내부 공지를 우선하라.\n");
        sb.append("[내부 공지]\n");
        for (var p : internalPosts) {
            sb.append("- 제목: ").append(p.getTitle()).append(" | 내용: ")
                    .append(StringUtils.abbreviate(p.getContent(), 800)).append("\n");
        }
        sb.append("[외부 자료]\n");
        for (var a : pages) {
            sb.append("- 제목: ").append(a.title()).append(" | 내용: ")
                    .append(StringUtils.abbreviate(a.text(), 1000)).append("\n");
        }

        String summary = flask.summarize(sb.toString());

        List<String> sources = new ArrayList<>();
        sources.addAll(internalPosts.stream().map(p -> p.getLink()).collect(Collectors.toList()));
        sources.addAll(externalLinks);

        return new AiSearchResponse(summary, sources);
    }

    /** Map-Reduce: 문서별 요약 → 최종 재요약 (추천) */
    @Cacheable(cacheNames = "summary", cacheManager = "redisCacheManager",
            key = "'mapreduce::' + #query + '::' + #maxExternal",
            unless = "#result == null || #result.tldr() == null || #result.tldr().isBlank()")
    public AiSearchDetailedResponse summarizeDetailed(String query, int maxExternal) {

        // 1) 내부 공지 5건
        var pageReq = PageRequest.of(0, 5, Sort.Direction.DESC, "crawledAt");
        var internal = postRepository.findAll(
                PostSpecification.containsKeyword(query), pageReq
        ).getContent();

        // 2) 외부 링크
        List<NaverSearchItemDto> news = naverSearchService.search("news", query, maxExternal);
        List<NaverSearchItemDto> blogs = naverSearchService.search("blog", query, maxExternal);
        List<NaverSearchItemDto> cafes = naverSearchService.search("cafearticle", query, Math.max(1, maxExternal - 1));

        List<String> extLinks = new ArrayList<>();
        extLinks.addAll(news.stream().map(NaverSearchItemDto::getLink).toList());
        extLinks.addAll(blogs.stream().map(NaverSearchItemDto::getLink).toList());
        extLinks.addAll(cafes.stream().map(NaverSearchItemDto::getLink).toList());
        extLinks = extLinks.stream().filter(Objects::nonNull).distinct().limit(maxExternal).toList();

        // 3) 문서별 요약
        List<PerDocSummary> items = new ArrayList<>();

        // 내부: DB 본문 사용
        for (var p : internal) {
            var one = perDocSummarizer.summarizeOne(
                    p.getLink(), p.getTitle(), "internal", null,
                    StringUtils.defaultString(p.getContent(), "")
            );
            if (one != null && StringUtils.isNotBlank(one.summary())) items.add(one);
        }

        // 외부: 본문 fetch → 요약
        for (String url : extLinks) {
            var page = contentFetcher.fetch(url);
            if (page == null || StringUtils.isBlank(page.text())) continue;
            var one = perDocSummarizer.summarizeOne(url, page.title(), "external", null, page.text());
            if (one != null && StringUtils.isNotBlank(one.summary())) items.add(one);
        }

        // 4) 최종 재요약
        String reducePrompt = """
                아래는 같은 주제의 여러 출처에서 뽑은 1~2문장 요약들이다.
                서로 상충하면 '내부 공지'를 우선하라.
                다음 형식으로 3~5줄 TL;DR을 작성하라:
                - 무엇이 발생/변경/공지되었는지
                - 대상/기간/장소/금액 등 핵심 수치
                - 해야 할 일(신청/참여/유의사항)
                자료:
                %s
                """.formatted(items.stream()
                .map(it -> "[%s] %s".formatted(it.sourceType(), it.summary()))
                .collect(Collectors.joining("\n")));

        String tldr = flask.summarize(reducePrompt);

        // 5) sources
        List<String> sources = new ArrayList<>();
        sources.addAll(internal.stream().map(p -> p.getLink()).toList());
        sources.addAll(extLinks);

        return new AiSearchDetailedResponse(tldr, items, sources);
    }

    /** 프롬프트 프리뷰(LLM 호출 전 확인용) */
    public String buildPromptPreview(String query, int maxExternal) {

        var pageReq = PageRequest.of(0, 5, Sort.Direction.DESC, "crawledAt");
        var internalPosts = postRepository.findAll(
                PostSpecification.containsKeyword(query), pageReq
        ).getContent();

        List<String> extLinks = new ArrayList<>();
        extLinks.addAll(naverSearchService.search("news", query, maxExternal).stream().map(NaverSearchItemDto::getLink).toList());
        extLinks.addAll(naverSearchService.search("blog", query, maxExternal).stream().map(NaverSearchItemDto::getLink).toList());
        extLinks.addAll(naverSearchService.search("cafearticle", query, Math.max(1, maxExternal - 1)).stream().map(NaverSearchItemDto::getLink).toList());
        extLinks = extLinks.stream().filter(Objects::nonNull).distinct().limit(maxExternal).toList();

        var pages = extLinks.stream().map(contentFetcher::fetch).filter(Objects::nonNull).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("다음 자료를 종합하여 요약하라. 내부 공지 우선.\n");
        sb.append("[내부 공지]\n");
        for (var p : internalPosts) {
            sb.append("- 제목: ").append(p.getTitle()).append(" | 내용: ")
                    .append(StringUtils.abbreviate(p.getContent(), 800)).append("\n");
        }
        sb.append("[외부 자료]\n");
        for (var a : pages) {
            sb.append("- 제목: ").append(a.title()).append(" | 내용: ")
                    .append(StringUtils.abbreviate(a.text(), 1000)).append("\n");
        }
        return sb.toString();
    }
}
