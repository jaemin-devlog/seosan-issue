package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.service.ContentFetcher.ArticleText;
import org.likelionhsu.backend.flask.FlaskSummarizeClient; // 네 프로젝트에 있는 요약 클라이언트로 맞춰주세요
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
    private final FlaskSummarizeClient flask; // LlmClient 없다면 이렇게 대체

    @Cacheable(cacheNames = "summary", cacheManager = "redisCacheManager",
            key = "#query + '::' + #maxExternal",
            unless = "#result == null || #result.summary == null || #result.summary.isBlank()")
    public AiSearchResponse summarize(String query, int maxExternal) {

        // 1) 내부 공지 최신 5건 (기존 Spec 재사용)
        var pageReq = PageRequest.of(0, 5, Sort.Direction.DESC, "crawledAt");
        var internal = postRepository.findAll(
                PostSpecification.containsKeyword(query), pageReq
        ).getContent(); // List<Post>

        // 2) 외부: 뉴스/블로그/카페에서 링크만 뽑기 (DTO 타입 직접 참조 X → var)
        List<String> extLinks = new ArrayList<>();
        extLinks.addAll(naverSearchService.search("news", query, maxExternal)
                .stream().map(item -> item.getLink()).toList());
        extLinks.addAll(naverSearchService.search("blog", query, maxExternal)
                .stream().map(item -> item.getLink()).toList());
        extLinks.addAll(naverSearchService.search("cafearticle", query, Math.max(1, maxExternal - 1))
                .stream().map(item -> item.getLink()).toList());

        // 3) 본문 추출 (중복 제거 후 상위 N만)
        List<ArticleText> pages = extLinks.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(maxExternal)
                .map(contentFetcher::fetch)
                .filter(Objects::nonNull)
                .toList();

        // 4) 요약 입력 (길이 제한)
        StringBuilder sb = new StringBuilder();
        sb.append("[내부 공지]\n");
        for (var p : internal) {
            sb.append("- 제목: ").append(p.getTitle()).append("\n")
                    .append(StringUtils.abbreviate(p.getContent(), 800)).append("\n\n");
        }
        sb.append("[외부 자료]\n");
        for (var a : pages) {
            sb.append("- 제목: ").append(a.title()).append("\n")
                    .append(StringUtils.abbreviate(a.text(), 1000)).append("\n\n");
        }

        String summary = flask.summarize(sb.toString());

        // 5) 출처 링크 (내부 + 외부)
        List<String> sources = new ArrayList<>();
        sources.addAll(internal.stream().map(p -> p.getLink()).collect(Collectors.toList()));
        sources.addAll(extLinks);

        return new AiSearchResponse(summary, sources);
    }

    public record AiSearchResponse(String summary, List<String> sources) {}
}
