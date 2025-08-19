package org.likelionhsu.backend.weather.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.likelionhsu.backend.weather.dto.WeatherCardDto;
import org.likelionhsu.backend.weather.dto.WeatherCardsResponse;
import org.likelionhsu.backend.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherService weatherService;

    @Test
    @DisplayName("GET /api/v1/weather/cards?city=서산시 전체 → 200 OK + JSON 구조 확인")
    void getCards_success() throws Exception {
        // Given: Service가 반환할 모의(Mock) 데이터 생성
        WeatherCardsResponse mockResponse = WeatherCardsResponse.builder()
                .city("서산시 전체")
                .baseDate("20250819")
                .baseTime("1100")
                .cards(List.of(
                        WeatherCardDto.builder().region("해미면").temperature(29.2).condition("맑음").build(),
                        WeatherCardDto.builder().region("성연면").temperature(28.7).condition("구름많음").build()
                ))
                .build();

        // Service의 getCardsByCity 메소드가 어떤 문자열로 호출되든 위에서 만든 mockResponse를 반환하도록 설정
        when(weatherService.getCardsByCity(anyString())).thenReturn(mockResponse);

        // When & Then: 실제 API를 호출하고 결과를 검증
        mockMvc.perform(get("/api/v1/weather/cards")
                        .param("city", "서산시 전체")) // 요청 파라미터 추가
                .andDo(print()) // 요청/응답 내용 출력
                .andExpect(status().isOk()) // HTTP 상태 코드가 200인지 확인
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 응답이 JSON 타입인지 확인
                .andExpect(jsonPath("$.city").value("서산시 전체")) // JSON 필드 값 검증
                .andExpect(jsonPath("$.cards[0].region").value("해미면"))
                .andExpect(jsonPath("$.cards[1].condition").value("구름많음"));
    }

    @Test
    @DisplayName("GET /api/v1/weather/cards city 파라미터 누락 시 → 400 Bad Request")
    void getCards_missingParam() throws Exception {
        // When & Then: city 파라미터 없이 API를 호출하면 400 에러가 발생하는지 확인
        mockMvc.perform(get("/api/v1/weather/cards"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
