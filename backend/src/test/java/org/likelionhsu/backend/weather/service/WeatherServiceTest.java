package org.likelionhsu.backend.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.backend.common.config.KmaApiConfig;
import org.likelionhsu.backend.common.config.KmaApiConfig.RegionCoordinate;
import org.likelionhsu.backend.common.exception.CustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.weather.dto.WeatherResponseDto;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.lenient; // lenient import 추가

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KmaApiConfig kmaApiConfig;

    @InjectMocks
    private WeatherService weatherService;

    private String mockKmaApiResponseSuccessPty0;
    private String mockKmaApiResponseSuccessPty1;
    private String mockKmaApiResponseFail;

    @BeforeEach
    void setUp() {
        // Mock KmaApiConfig properties
        lenient().when(kmaApiConfig.getServiceKey()).thenReturn("testServiceKey");
        lenient().when(kmaApiConfig.getBaseUrl()).thenReturn("http://apis.data.go.kr/1360000/VilageFcstInfoServiceU");

        RegionCoordinate seosan = new RegionCoordinate();
        seosan.setName("서산시");
        seosan.setNx(50);
        seosan.setNy(120);

        RegionCoordinate taean = new RegionCoordinate();
        taean.setName("태안군");
        taean.setNx(51);
        taean.setNy(121);

        lenient().when(kmaApiConfig.getGridCoords()).thenReturn(Arrays.asList(seosan, taean));

        // Mock KMA API successful response (PTY 0 - 맑음)
        mockKmaApiResponseSuccessPty0 = "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_SERVICE\"},\"body\":{\"dataType\":\"JSON\",\"items\":{\"item\":[{\"baseDate\":\"20230101\",\"baseTime\":\"0600\",\"category\":\"T1H\",\"obsrValue\":\"25.0\"},{\"baseDate\":\"20230101\",\"baseTime\":\"0600\",\"category\":\"REH\",\"obsrValue\":\"70\"},{\"baseDate\":\"20230101\",\"baseTime\":\"0600\",\"category\":\"PTY\",\"obsrValue\":\"0\"},{\"baseDate\":\"20230101\",\"baseTime\":\"0600\",\"category\":\"WSD\",\"obsrValue\":\"3.5\"}]},\"pageNo\":1,\"numOfRows\":100,\"totalCount\":4}}}";

        // Mock KMA API successful response (PTY 1 - 비)
        mockKmaApiResponseSuccessPty1 = "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_SERVICE\"},\"body\":{\"dataType\":\"JSON\",\"items\":{\"item\":[{\"baseDate\":\"20230101\",\"baseTime\":\"0600\",\"category\":\"T1H\",\"obsrValue\":\"20.0\"},{\"baseDate\":\"20230101\",\"baseTime\":\"0600\",\"category\":\"REH\",\"obsrValue\":\"90\"},{\"baseDate\":\"20230101\",\"baseTime\":\"0600\",\"category\":\"PTY\",\"obsrValue\":\"1\"},{\"baseDate\":\"20230101\",\"baseTime\":\"0600\",\"category\":\"WSD\",\"obsrValue\":\"5.0\"}]},\"pageNo\":1,\"numOfRows\":100,\"totalCount\":4}}}";

        // Mock KMA API failed response
        mockKmaApiResponseFail = "{\"response\":{\"header\":{\"resultCode\":\"99\",\"resultMsg\":\"APPLICATION_ERROR\"},\"body\":{}}}";
    }

    @Test
    @DisplayName("날씨 정보 조회 성공 - PTY 0 (맑음)")
    void getWeather_successPty0() throws Exception {
        // Given
        String region = "서산시";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Content-Type 설정
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockKmaApiResponseSuccessPty0, headers, HttpStatus.OK);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);

        JsonNode mockJsonNode = new ObjectMapper().readTree(mockKmaApiResponseSuccessPty0);
        when(objectMapper.readTree(any(String.class))).thenReturn(mockJsonNode);

        // When
        WeatherResponseDto result = weatherService.getWeather(region);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRegion()).isEqualTo(region);
        assertThat(result.getTemperature()).isEqualTo("25.0");
        assertThat(result.getHumidity()).isEqualTo("70");
        assertThat(result.getSkyDescription()).isEqualTo("맑음");
        assertThat(result.getWindSpeed()).isEqualTo("3.5");
    }

    @Test
    @DisplayName("날씨 정보 조회 성공 - PTY 1 (비)")
    void getWeather_successPty1() throws Exception {
        // Given
        String region = "서산시";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Content-Type 설정
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockKmaApiResponseSuccessPty1, headers, HttpStatus.OK);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);

        JsonNode mockJsonNode = new ObjectMapper().readTree(mockKmaApiResponseSuccessPty1);
        when(objectMapper.readTree(any(String.class))).thenReturn(mockJsonNode);

        // When
        WeatherResponseDto result = weatherService.getWeather(region);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRegion()).isEqualTo(region);
        assertThat(result.getTemperature()).isEqualTo("20.0");
        assertThat(result.getHumidity()).isEqualTo("90");
        assertThat(result.getSkyDescription()).isEqualTo("비");
        assertThat(result.getWindSpeed()).isEqualTo("5.0");
    }

    @Test
    @DisplayName("지원하지 않는 지역으로 날씨 정보 조회 시 예외 발생")
    void getWeather_unsupportedRegion() {
        // Given
        String region = "없는지역";

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                weatherService.getWeather(region)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(exception.getMessage()).contains("지원하지 않는 지역입니다: " + region); // contains로 변경
    }

    @Test
    @DisplayName("기상청 API 응답 형식 오류 시 예외 발생 (JSON 아님)")
    void getWeather_apiResponseContentTypeError() throws Exception { // throws Exception 추가
        // Given
        String region = "서산시";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN); // Non-JSON content type
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("Not a JSON", headers, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                weatherService.getWeather(region)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).contains("기상청 API 응답 형식 이 올바르지 않습니다. (JSON 아님)"); // contains로 변경
        verify(objectMapper, times(0)).readTree(anyString()); // readTree가 호출되지 않았음을 검증
    }

    @Test
    @DisplayName("기상청 API 결과 코드 오류 시 예외 발생 (00 아님)")
    void getWeather_apiResultCodeError() throws Exception {
        // Given
        String region = "서산시";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Content-Type 설정
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockKmaApiResponseFail, headers, HttpStatus.OK);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);

        JsonNode mockJsonNode = new ObjectMapper().readTree(mockKmaApiResponseFail);
        when(objectMapper.readTree(any(String.class))).thenReturn(mockJsonNode);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                weatherService.getWeather(region)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).contains("기상청 API 호출 실패: APPLICATION_ERROR"); // contains로 변경
    }

    @Test
    @DisplayName("기상청 API 호출 중 일반 예외 발생 시 처리")
    void getWeather_generalException() {
        // Given
        String region = "서산시";
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("API call failed"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                weatherService.getWeather(region)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).contains("API call failed"); // ErrorCode의 기본 메시지 대신 RuntimeException의 메시지를 포함하도록 변경
    }
}