//package org.likelionhsu.backend.weather.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.likelionhsu.backend.weather.dto.WeatherResponseDto;
//import org.likelionhsu.backend.weather.service.WeatherService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(WeatherController.class)
//class WeatherControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private WeatherService weatherService;
//
//    @Test
//    @DisplayName("날씨 정보 조회 성공")
//    void getWeather_success() throws Exception {
//        // Given
//        String region = "서산시";
//        WeatherResponseDto mockResponse = WeatherResponseDto.builder()
//                .region(region)
//                .temperature("25.0")
//                .humidity("70")
//                .sky("맑음")
//                .pty("0") // precipitation 대신 pty 사용
//                .windSpeed("3.5")
//                .build();
//
//        when(weatherService.getWeather(anyString())).thenReturn(mockResponse);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/weather")
//                        .param("region", region))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.region").value(region))
//                .andExpect(jsonPath("$.temperature").value("25.0"));
//    }
//
//    @Test
//    @DisplayName("날씨 정보 조회 실패 (서비스 예외 발생)")
//    void getWeather_failure() throws Exception {
//        // Given
//        String region = "잘못된지역";
//        // 예외 처리를 위한 ErrorCode가 없으므로, RuntimeException으로 대체
//        when(weatherService.getWeather(anyString())).thenThrow(new RuntimeException("날씨 정보를 찾을 수 없습니다."));
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/weather")
//                        .param("region", region))
//                .andDo(print())
//                .andExpect(status().isInternalServerError()); // RuntimeException은 기본적으로 500 Internal Server Error로 매핑됩니다.
//    }
//}