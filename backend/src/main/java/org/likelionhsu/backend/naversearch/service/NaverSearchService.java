package org.likelionhsu.backend.naversearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.naversearch.NaverSearchItemDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NaverSearchService {

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final ObjectMapper objectMapper;
    private final WebClient external;

    // ★ 생성자 파라미터에 Qualifier 명시
    public NaverSearchService(
            ObjectMapper objectMapper,
            @Qualifier("externalWebClient") WebClient external
    ) {
        this.objectMapper = objectMapper;
        this.external = external;
    }

    private static final String NAVER_HOST = "openapi.naver.com";
    private static final String DATALAB_PATH = "/v1/datalab/search";
    private static final String SEARCH_BASE_PATH = "/v1/search";

    /* --------------------- DataLab --------------------- */

    public JsonNode getDailyTrends(String startDate, String endDate, List<Map<String, Object>> keywordGroups) {
        return callNaverDatalabApi("date", startDate, endDate, keywordGroups);
    }

    public JsonNode getWeeklyTrends(String startDate, String endDate, List<Map<String, Object>> keywordGroups) {
        return callNaverDatalabApi("week", startDate, endDate, keywordGroups);
    }

    private JsonNode callNaverDatalabApi(String timeUnit, String startDate, String endDate,
                                         List<Map<String, Object>> keywordGroups) {
        Map<String, Object> body = Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "timeUnit", timeUnit,
                "keywordGroups", keywordGroups
        );

        String json = external.post()
                .uri(uri -> uri.scheme("https").host(NAVER_HOST).path(DATALAB_PATH).build())
                .headers(this::applyAuthHeadersJson)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                .block();

        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("네이버 데이터랩 응답 파싱 실패", e);
            throw new RuntimeException("네이버 데이터랩 응답 파싱 실패", e);
        }
    }

    /* --------------------- Search --------------------- */

    @Cacheable(cacheNames = "discovery", cacheManager = "caffeineCacheManager",
            key = "#type + '::' + #query + '::' + #display",
            unless = "#result == null || #result.isEmpty()")
    public List<NaverSearchItemDto> search(String type, String query, int display) {
        int d = Math.max(1, display);

        String json = external.get()
                .uri(uri -> uri.scheme("https")
                        .host(NAVER_HOST)
                        .path(SEARCH_BASE_PATH + "/" + type + ".json")
                        .queryParam("query", query)
                        .queryParam("display", d)
                        .queryParam("start", 1)
                        .queryParam("sort", "sim")
                        .build())
                .headers(this::applyAuthHeaders)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                .block();


        List<NaverSearchItemDto> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode item : root.path("items")) {
                list.add(new NaverSearchItemDto(
                        item.path("title").asText(),
                        item.path("description").asText(),
                        item.path("link").asText(),
                        item.path("originallink").asText(), // ★ 추가
                        type
                ));
            }
        } catch (Exception e) {
            log.error("네이버 검색 응답 파싱 실패", e);
        }
        return list;
    }

    /* --------------------- Helpers --------------------- */

    private void applyAuthHeaders(HttpHeaders h) {
        h.set("X-Naver-Client-Id", clientId);
        h.set("X-Naver-Client-Secret", clientSecret);
    }

    private void applyAuthHeadersJson(HttpHeaders h) {
        applyAuthHeaders(h);
        h.setContentType(MediaType.APPLICATION_JSON);
    }
}
