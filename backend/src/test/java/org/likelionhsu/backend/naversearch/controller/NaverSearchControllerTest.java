package org.likelionhsu.backend.naversearch.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.likelionhsu.backend.naversearch.service.NaverSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NaverSearchController.class)
class NaverSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NaverSearchService naverSearchService;

    @Test
    @DisplayName("일간 트렌드 조회 API 성공")
    void getDailyTrends_success() throws Exception {
        // Given
        JsonNode mockResponse = objectMapper.readTree("{\"results\":[]}");
        when(naverSearchService.getDailyTrends("2023-01-01", "2023-01-07", List.of(
                Map.of("groupName", "Technology", "keywords", List.of("AI", "Machine Learning"))
        ))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/naver-search/daily-trend")
                        .param("startDate", "2023-01-01")
                        .param("endDate", "2023-01-07")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(
                                Map.of("groupName", "Technology", "keywords", List.of("AI", "Machine Learning"))
                        ))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("주간 트렌드 조회 API 성공")
    void getWeeklyTrends_success() throws Exception {
        // Given
        JsonNode mockResponse = objectMapper.readTree("{\"results\":[]}");
        when(naverSearchService.getWeeklyTrends("2023-01-01", "2023-01-31", List.of(
                Map.of("groupName", "Health", "keywords", List.of("Yoga", "Meditation"))
        ))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/naver-search/weekly-trend")
                        .param("startDate", "2023-01-01")
                        .param("endDate", "2023-01-31")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(
                                Map.of("groupName", "Health", "keywords", List.of("Yoga", "Meditation"))
                        ))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }
}