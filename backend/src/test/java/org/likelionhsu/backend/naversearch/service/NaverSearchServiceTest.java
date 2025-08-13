package org.likelionhsu.backend.naversearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaverSearchServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NaverSearchService naverSearchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(naverSearchService, "clientId", "testClientId");
        ReflectionTestUtils.setField(naverSearchService, "clientSecret", "testClientSecret");
    }

    @Test
    @DisplayName("일간 트렌드 조회 성공")
    void getDailyTrends_success() throws Exception {
        // Given
        String mockResponse = "{\"results\":[]}";
        JsonNode mockJsonNode = new ObjectMapper().readTree(mockResponse);

        when(restTemplate.exchange(any(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));
        when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);

        // When
        JsonNode result = naverSearchService.getDailyTrends("2023-01-01", "2023-01-07", List.of(
                Map.of("groupName", "Technology", "keywords", List.of("AI", "Machine Learning"))
        ));

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("주간 트렌드 조회 성공")
    void getWeeklyTrends_success() throws Exception {
        // Given
        String mockResponse = "{\"results\":[]}";
        JsonNode mockJsonNode = new ObjectMapper().readTree(mockResponse);

        when(restTemplate.exchange(any(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));
        when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);

        // When
        JsonNode result = naverSearchService.getWeeklyTrends("2023-01-01", "2023-01-31", List.of(
                Map.of("groupName", "Health", "keywords", List.of("Yoga", "Meditation"))
        ));

        // Then
        assertThat(result).isNotNull();
    }
}