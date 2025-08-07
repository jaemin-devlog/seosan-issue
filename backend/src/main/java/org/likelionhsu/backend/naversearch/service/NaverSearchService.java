package org.likelionhsu.backend.naversearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.common.exception.CustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Emotion;
import org.likelionhsu.backend.post.domain.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    private static final String NAVER_SEARCH_API_URL = "https://openapi.naver.com/v1/search/";

    public List<Post> searchNaverBlogsAndCafes(String query, int display, int start, String region) {
        List<Post> posts = new ArrayList<>();

        // 블로그 검색
        posts.addAll(callNaverSearchApi("blog.json", query, display, start, Category.BLOG, region));
        // 카페 검색
        posts.addAll(callNaverSearchApi("cafearticle.json", query, display, start, Category.CAFE, region));

        return posts;
    }

    private List<Post> callNaverSearchApi(String apiPath, String query, int display, int start, Category category, String region) {
        List<Post> posts = new ArrayList<>();
        URI uri = UriComponentsBuilder.fromUriString(NAVER_SEARCH_API_URL + apiPath)
                .queryParam("query", query)
                .queryParam("display", display)
                .queryParam("start", start)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("items");

            for (JsonNode item : items) {
                String title = item.path("title").asText().replaceAll("<(/)?([a-zA-Z]*)>", ""); // HTML 태그 제거
                String link = item.path("link").asText();
                String description = item.path("description").asText().replaceAll("<(/)?([a-zA-Z]*)>", ""); // HTML 태그 제거
                String postdate = item.path("postdate").asText(); // YYYYMMDD 형식

                // pubDate를 YYYY-MM-DD 형식으로 변환
                String formattedPubDate = null;
                if (postdate != null && postdate.length() == 8) {
                    formattedPubDate = postdate.substring(0, 4) + "-" + postdate.substring(4, 6) + "-" + postdate.substring(6, 8);
                } else { // postdate가 없는 경우 (주로 카페글)
                    formattedPubDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }

                String bloggerName = item.path("bloggername").asText(); // 블로그 이름

                // TODO: 감정 분석 AI 연동 (현재는 NEUTRAL로 고정)
                posts.add(Post.builder()
                        .title(title)
                        .content(description)
                        .link(link)
                        .pubDate(formattedPubDate)
                        .region(region) // 전달받은 region 정보 반영
                        .category(category)
                        .emotion(Emotion.NEUTRAL) // 초기값, 추후 AI 분석 결과 반영
                        .department(bloggerName) // 블로그 이름 저장
                        .views(null) // 네이버 검색 결과에는 조회수 정보 없음
                        .crawledAt(LocalDateTime.now())
                        .build());
            }
        } catch (HttpClientErrorException e) {
            log.error("네이버 API 클라이언트 오류 ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is4xxClientError()) {
                throw new CustomException(ErrorCode.NAVER_API_BAD_REQUEST, e);
            } else if (e.getStatusCode().is5xxServerError()) {
                throw new CustomException(ErrorCode.NAVER_API_SERVER_ERROR, e);
            } else {
                throw new CustomException(ErrorCode.NAVER_API_UNKNOWN_ERROR, e);
            }
        } catch (HttpServerErrorException e) {
            log.error("네이버 API 서버 오류 ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.NAVER_API_SERVER_ERROR, e);
        } catch (Exception e) {
            log.error("네이버 검색 API 호출 중 알 수 없는 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.NAVER_API_UNKNOWN_ERROR, e);
        }
        return posts;
    }
}
