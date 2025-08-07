package org.likelionhsu.backend.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.common.config.KmaApiConfig;
import org.likelionhsu.backend.common.config.KmaApiConfig.RegionCoordinate;
import org.likelionhsu.backend.common.exception.CustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.weather.dto.WeatherResponseDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KmaApiConfig kmaApiConfig;

    private static final String KMA_API_PATH = "getUltraSrtNcst"; // 초단기실황조회

    public WeatherResponseDto getWeather(String region) {
        log.info("KmaApiProperties gridCoords: {}", kmaApiConfig.getGridCoords());

        RegionCoordinate selectedCoords = null;
        for (RegionCoordinate coord : kmaApiConfig.getGridCoords()) {
            if (coord.getName().equals(region)) {
                selectedCoords = coord;
                break;
            }
        }

        if (selectedCoords == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 지역입니다: " + region); // 지원하지 않는 지역은 입력 값 오류로 처리
        }

        int nx = selectedCoords.getNx();
        int ny = selectedCoords.getNy();

        // 현재 시간 기준으로 발표 일자와 시각 계산
        LocalDateTime now = LocalDateTime.now();
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 초단기실황 API는 매시각 10분 이후 호출 가능 (예: 04:15 -> base_time 0400, 04:05 -> base_time 0300)
        String baseTime = getUltraSrtBaseTime(now);

        String serviceKey = kmaApiConfig.getServiceKey(); // 이미 인코딩된 키 사용

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(kmaApiConfig.getBaseUrl() + "/" + KMA_API_PATH) // 여기에 '/' 추가
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "100") // 모든 예보 항목을 가져오기 위해 충분히 큰 값 설정
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny);

        try {
            String apiUrl = uriBuilder.toUriString() + "&serviceKey=" + serviceKey;
            URI uri = new URI(apiUrl);
            log.info("기상청 API 호출 URL: {}", uri);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            log.info("기상청 API 응답: {}", response.getBody());

            // 응답 Content-Type이 JSON이 아니면 예외 발생
            if (response.getHeaders().getContentType() == null || !response.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)) {
                log.error("기상청 API 응답 타입 오류: 예상 JSON, 실제 {}", response.getHeaders().getContentType());
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "기상청 API 응답 형식 이 올바르지 않습니다. (JSON 아님)");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode header = root.path("response").path("header");
            String resultCode = header.path("resultCode").asText();
            String resultMsg = header.path("resultMsg").asText();

            if (!"00".equals(resultCode)) {
                log.error("기상청 API 호출 실패: {} - {}", resultCode, resultMsg);
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "기상청 API 호출 실패: " + resultMsg); // API 호출 실패는 서버 오류로 처리
            }

            JsonNode items = root.path("response").path("body").path("items").path("item");
            Map<String, String> weatherData = new HashMap<>();

            for (JsonNode item : items) {
                String category = item.path("category").asText();
                String fcstValue = item.path("obsrValue").asText(); // 초단기실황은 obsrValue
                weatherData.put(category, fcstValue);
            }

            return WeatherResponseDto.builder()
                    .baseDate(baseDate)
                    .baseTime(baseTime)
                    .region(region)
                    .temperature(weatherData.getOrDefault("T1H", "N/A")) // 1시간 기온
                    .humidity(weatherData.getOrDefault("REH", "N/A"))    // 습도
                    .sky(weatherData.getOrDefault("SKY", "N/A"))         // 하늘 상태 (초단기실황에는 없음)
                    .pty(weatherData.getOrDefault("PTY", "N/A"))         // 강수 형태 (초단기실황에는 없음)
                    .skyDescription(getSkyDescription(weatherData.getOrDefault("PTY", "N/A"))) // PTY 값으로 skyDescription 설정
                    .windSpeed(weatherData.getOrDefault("WSD", "N/A"))   // 풍속
                    .windDirection(weatherData.getOrDefault("VEC", "N/A")) // 풍향 (초단기실황에는 VEC만 있음)
                    .build();

        } catch (Exception e) {
            log.error("날씨 정보 파싱 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage()); // 원인 예외의 메시지를 직접 전달
        }
    }

    private String getUltraSrtBaseTime(LocalDateTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();

        // 매시각 10분 이후 호출 가능
        if (minute < 10) {
            // 현재 시각이 정시 10분 전이면 이전 시간의 00분으로 설정
            hour = hour - 1;
            if (hour < 0) hour = 23; // 자정 이전이면 전날 23시
            return String.format("%02d00", hour);
        } else {
            // 현재 시각이 정시 10분 이후면 현재 시간의 00분으로 설정
            return String.format("%02d00", hour);
        }
    }

    // 기존 getBaseTime은 사용하지 않음
    private String getBaseTime(LocalDateTime now) {
        return null; // 사용하지 않으므로 null 반환 또는 제거
    }

    private String getSkyDescription(String ptyCode) {
        // PTY (강수 형태) codes: 0: 없음, 1: 비, 2: 비/눈, 3: 눈, 4: 소나기, 5: 빗방울, 6: 빗방울눈날림, 7: 눈날림
        if ("1".equals(ptyCode)) return "비";
        if ("2".equals(ptyCode)) return "비/눈";
        if ("3".equals(ptyCode)) return "눈";
        if ("4".equals(ptyCode)) return "소나기";
        if ("5".equals(ptyCode)) return "빗방울";
        if ("6".equals(ptyCode)) return "빗방울눈날림";
        if ("7".equals(ptyCode)) return "눈날림";

        // PTY가 0 (강수 없음)일 경우, 하늘 상태는 이 API에서 제공되지 않으므로 기본적으로 "맑음"으로 설정
        if ("0".equals(ptyCode)) return "맑음";

        return "정보 없음"; // 알 수 없는 코드
    }
}


