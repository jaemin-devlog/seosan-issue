package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.ai.clean.PreCleaner;
import org.likelionhsu.backend.ai.dto.AiSearchResponse;
import org.likelionhsu.backend.ai.dto.AiSearchDetailedResponse;
import org.likelionhsu.backend.ai.dto.PerDocSummary;
import org.likelionhsu.backend.flask.FlaskSummarizeClient;
import org.likelionhsu.backend.ai.prompt.PromptTemplates;
import org.likelionhsu.backend.ai.validate.PostValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummarizationOrchestrator {

    private final PreCleaner preCleaner;
    private final PromptTemplates prompts;
    private final FlaskSummarizeClient flask;
    private final PostValidator postValidator;

    @Value("${ai.prompt.one-line-tldr-chars:28}")
    private int oneLineChars;

    // ===== 리스트 요약 (원샷) =====
    public Mono<AiSearchResponse> oneshot(List<String> candidateDocs, List<String> sources) {
        List<String> flags = new ArrayList<>();

        List<String> cleanedDocs = candidateDocs.stream()
                .map(preCleaner::clean)
                .peek(r -> flags.addAll(r.noiseFlags()))
                .map(PreCleaner.CleanResult::text)
                .filter(s -> s != null && !s.trim().isEmpty()) // JDK8 호환
                .collect(Collectors.toList());

        if (cleanedDocs.isEmpty()) {
            return Mono.empty(); // 컨트롤러에서 204
        }

        String joined = String.join("\n\n", cleanedDocs);
        String sys = prompts.oneshotSystem(oneLineChars);
        String usr = prompts.oneshotUser(joined);

        return flask.summarize(sys, usr)
                .map(postValidator::validateOneLine)
                .map(v -> {
                    AiSearchResponse res = new AiSearchResponse(null, sources);
                    res.setNoiseFlags(flags);
                    if (!v.ok()) {
                        res.setAbstained(true);
                        res.setAbstainReason(v.reason());
                        return res;
                    }
                    // 🔑 여기서 sanitize 처리
                    res.setSummary(sanitizeResponse(v.content()));
                    res.setAbstained(false);
                    return res;
                })
                .onErrorResume(ex -> {
                    AiSearchResponse res = new AiSearchResponse(null, sources);
                    res.setAbstained(true);
                    res.setAbstainReason("LLM_CALL_ERROR");
                    res.setNoiseFlags(flags);
                    return Mono.just(res);
                });
    }

    // ===== 상세요약 (map-reduce) =====
    public Mono<AiSearchDetailedResponse> detailed(List<Doc> docs, List<String> sources) {
        List<String> flags = new ArrayList<>();
        List<PerDocSummary> items = new ArrayList<>();

        // Map 단계
        for (Doc d : docs) {
            var cleaned = preCleaner.clean(d.body());
            flags.addAll(cleaned.noiseFlags());

            if (cleaned.text().trim().isEmpty()) {
                items.add(new PerDocSummary(
                        d.url(), d.title(), d.sourceType(), d.publishedAt(),
                        null,                      // summary 없음
                        d.body()                   // content 폴백
                ));
                continue;
            }

            String sys = prompts.reduceSystem();
            String usr = prompts.mapUser(cleaned.text());

            String mapOut = flask.summarize(sys, usr).blockOptional().orElse("");
            String perDocSummary = mapOut == null ? null : mapOut.trim();

            items.add(new PerDocSummary(
                    d.url(), d.title(), d.sourceType(), d.publishedAt(),
                    perDocSummary,      // summary
                    null                // content 폴백 불필요
            ));
        }

        // Reduce 단계: joinedFacts 만들기
        String joinedFacts = items.stream()
                .map(PerDocSummary::summary)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));

        // 🔑 요기서 바로 컷
        if (joinedFacts.isEmpty()) {
            return Mono.empty();  // 컨트롤러에서 204로 처리
        }

        String sys = prompts.reduceSystem();
        String usr = prompts.reduceUser(joinedFacts);
        String tldr = flask.summarize(sys, usr).blockOptional().orElse("").trim();
        tldr = sanitizeResponse(tldr); // 🔑 추가

        if (tldr.isEmpty()) {
            return Mono.empty();  // 컨트롤러에서 204
        }

        return Mono.just(new AiSearchDetailedResponse(tldr, items, sources));
    }

    public Map<String, String> buildPromptPreview(String query, int maxExternal) {
        List<String> flags = new ArrayList<>();

        // 1. 내부/외부 문서 수집 로직 자리 (지금은 예시로 query만 넣어둠)
        //    실제로는 PostRepository 검색 + 외부 검색 API 호출해서 본문 가져와야 함
        List<String> candidateDocs = List.of(
                "예시 내부 문서: " + query,
                "예시 외부 뉴스: " + query
        );

        // 2. Pre-clean
        List<String> cleanedDocs = candidateDocs.stream()
                .map(preCleaner::clean)
                .peek(r -> flags.addAll(r.noiseFlags()))
                .map(PreCleaner.CleanResult::text)
                .filter(s -> s != null && !s.trim().isEmpty())
                .toList();

        String joined = String.join("\n\n", cleanedDocs);

        // 3. PromptTemplates 조합
        String sys = prompts.oneshotSystem(oneLineChars);
        String usr = prompts.oneshotUser(joined);

        // 4. 미리보기 결과를 Map으로 반환
        return Map.of(
                "system", sys,
                "user", usr,
                "noiseFlags", String.join(",", flags)
        );
    }

    // 외부/내부 본문 운반용
    public record Doc(String url, String title, String sourceType, String publishedAt, String body) {}

    private String sanitizeResponse(String input) {
        if (input == null) return null;
        // 한글, 영어, 숫자, 공백, ., , , ! 만 허용
        return input.replaceAll("[^가-힣a-zA-Z0-9 .,!]", "");
    }
}
