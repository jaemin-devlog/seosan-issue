package org.likelionhsu.backend.naversearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.naversearch.NaverSearchItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaverSearchService {

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String NAVER_DATALAB_API_URL = "https://openapi.naver.com/v1/datalab/search";
    private static final String BASE = "https://openapi.naver.com/v1/search";

    public JsonNode getDailyTrends(String startDate, String endDate, List<Map<String, Object>> keywordGroups) {
        return callNaverDatalabApi("date", startDate, endDate, keywordGroups);
    }

    public JsonNode getWeeklyTrends(String startDate, String endDate, List<Map<String, Object>> keywordGroups) {
        return callNaverDatalabApi("week", startDate, endDate, keywordGroups);
    }

    private JsonNode callNaverDatalabApi(String timeUnit, String startDate, String endDate, List<Map<String, Object>> keywordGroups) {
        URI uri = UriComponentsBuilder.fromUriString(NAVER_DATALAB_API_URL).build().toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "timeUnit", timeUnit,
                "keywordGroups", keywordGroups
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("네이버 데이터랩 API 호출 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("네이버 데이터랩 API 호출 실패", e);
        }
    }

    @Cacheable(cacheNames = "discovery", cacheManager = "caffeineCacheManager",
            key = "#type + '::' + #query + '::' + #display",
            unless = "#result == null || #result.isEmpty()")
    public List<NaverSearchItemDto> search(String type, String query, int display) {
        // ✅ UriComponentsBuilder가 인코딩하도록 맡기는 걸 권장
        var uri = UriComponentsBuilder
                .fromHttpUrl(BASE + "/" + type + ".json")
                .queryParam("query", query)
                .queryParam("display", display)
                .queryParam("start", 1)
                .queryParam("sort", "sim")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        var headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        var res = restTemplate.exchange(uri, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        var list = new java.util.ArrayList<NaverSearchItemDto>();
        try {
            var root = objectMapper.readTree(res.getBody());
            for (JsonNode item : root.path("items")) {
                list.add(new NaverSearchItemDto(
                        item.path("title").asText(),
                        item.path("description").asText(),
                        item.path("link").asText(),
                        type
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }
}