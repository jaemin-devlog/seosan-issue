package org.likelionhsu.backend.naversearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.backend.common.exception.CustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Post;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaverSearchServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NaverSearchService naverSearchService;

    private String mockNaverApiResponse;

    @BeforeEach
    void setUp() {
        // @Value 필드 주입
        ReflectionTestUtils.setField(naverSearchService, "clientId", "testClientId");
        ReflectionTestUtils.setField(naverSearchService, "clientSecret", "testClientSecret");

        // JSON 문자열에서 \/ 대신 / 사용
        mockNaverApiResponse = "{\"lastBuildDate\":\"Thu, 07 Aug 2025 15:00:00 +0900\",\"total\":2,\"start\":1,\"display\":2,\"items\":[{\"title\":\"<b>테스트</b> 블로그 1\",\"link\":\"http://testblog1.com\",\"description\":\"테스트 내용 1\",\"bloggername\":\"테스터1\",\"postdate\":\"20230101\"},{\"title\":\"<b>테스트</b> 카페 1\",\"link\":\"http://testcafe1.com\",\"description\":\"테스트 내용 2\",\"cafename\":\"테스터2\",\"postdate\":\"20230102\"}]}";
    }

    @Test
    @DisplayName("네이버 블로그/카페 검색 성공")
    void searchNaverBlogsAndCafes_success() throws Exception {
        // Given
        // ObjectMapper가 JSON을 파싱하도록 Mocking
        JsonNode mockJsonNode = new ObjectMapper().readTree(mockNaverApiResponse);
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockNaverApiResponse, HttpStatus.OK);
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);

        // When
        List<Post> result = naverSearchService.searchNaverBlogsAndCafes("query", 10, 1, "region");

        // Then
        assertThat(result).hasSize(4); // 블로그 2개, 카페 2개 (각각 2개씩 반환하도록 mockNaverApiResponse 설정)
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 블로그 1");
        assertThat(result.get(2).getTitle()).isEqualTo("테스트 블로그 1"); // 두 번째 호출도 동일한 응답을 반환하므로

        // callNaverSearchApi가 blog.json과 cafearticle.json에 대해 각각 한 번씩 호출되었는지 확인
        verify(restTemplate, times(2)).exchange(any(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("네이버 블로그/카페 검색 실패 - HttpClientErrorException (4xx)")
    void searchNaverBlogsAndCafes_httpClientErrorException() {
        // Given
        HttpClientErrorException clientError = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", "Error Body".getBytes(), null);
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(clientError);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                naverSearchService.searchNaverBlogsAndCafes("query", 10, 1, "region")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NAVER_API_BAD_REQUEST);
    }

    @Test
    @DisplayName("네이버 블로그/카페 검색 실패 - HttpServerErrorException (5xx)")
    void searchNaverBlogsAndCafes_httpServerErrorException() {
        // Given
        HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Error Body".getBytes(), null);
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(serverError);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                naverSearchService.searchNaverBlogsAndCafes("query", 10, 1, "region")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NAVER_API_SERVER_ERROR);
    }

    @Test
    @DisplayName("네이버 블로그/카페 검색 실패 - 일반 Exception")
    void searchNaverBlogsAndCafes_generalException() {
        // Given
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("General Error"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                naverSearchService.searchNaverBlogsAndCafes("query", 10, 1, "region")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NAVER_API_UNKNOWN_ERROR);
    }
}
