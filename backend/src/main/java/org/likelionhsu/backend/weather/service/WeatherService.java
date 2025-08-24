package org.likelionhsu.backend.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.common.config.KmaApiConfig;
import org.likelionhsu.backend.common.config.KmaApiConfig.RegionCoordinate;
import org.likelionhsu.backend.common.exception.CustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.weather.dto.WeatherCardDto;
import org.likelionhsu.backend.weather.dto.WeatherCardsResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class WeatherService {

    private final WebClient external;
    private final ObjectMapper objectMapper;
    private final KmaApiConfig kmaApiConfig;

    public WeatherService(
            ObjectMapper objectMapper,
            KmaApiConfig kmaApiConfig,
            @Qualifier("externalWebClient") WebClient external
    ) {
        this.objectMapper = objectMapper;
        this.kmaApiConfig = kmaApiConfig;
        this.external = external;
    }

    private static final String NCST_PATH = "getUltraSrtNcst";  // 초단기실황
    private static final String FCST_PATH = "getUltraSrtFcst";  // 초단기예보

    @Getter
    @AllArgsConstructor
    private static class KmaResponse {
        private final String resultCode;
        private final JsonNode items;
        private final Map<String, String> dataMap;
    }

    /** 도시 단위 카드 묶음 조회 */
    public WeatherCardsResponse getCardsByCity(String city) {
        var coords = Optional.ofNullable(kmaApiConfig.getGridCoords())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE, "grid-coords가 비어 있습니다."));

        var pool = Executors.newFixedThreadPool(Math.min(6, coords.size()));
        try {
            var futures = coords.stream()
                    .map(rc -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return buildCard(rc.getName());
                        } catch (CustomException e) {
                            log.warn("날씨 카드 생성 실패 (좌표: {}), 스킵합니다. 원인: {}", rc.getName(), e.getMessage());
                            return null;
                        }
                    }, pool))
                    .toList();

            List<WeatherCardDto> cards = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

            LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime();
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = ncstBaseTime(now)[0];

            // ✅ 자정 보정
            if (now.getHour() == 0 && "2300".equals(baseTime)) {
                baseDate = now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }

            return WeatherCardsResponse.builder()
                    .city(city)
                    .baseDate(baseDate)
                    .baseTime(baseTime)
                    .cards(cards)
                    .build();
        } finally {
            pool.shutdown();
        }
    }

    /** 개별 지역 카드 생성 */
    private WeatherCardDto buildCard(String region) {
        RegionCoordinate coord = kmaApiConfig.getGridCoords().stream()
                .filter(c -> c.getName().equals(region))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE, "존재하지 않는 지역입니다: " + region));

        LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime();
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = ncstBaseTime(now)[0];

        // ✅ 자정 보정
        if (now.getHour() == 0 && "2300".equals(baseTime)) {
            baseDate = now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        // 1) 초단기실황 (폴백 적용)
        KmaResponse ncstRes = null;
        for (String bt : ncstBaseTime(now)) {
            ncstRes = callKma(NCST_PATH, baseDate, bt, coord.getNx(), coord.getNy(), true);
            if ("00".equals(ncstRes.getResultCode()) && ncstRes.getItems().size() > 0) break;
        }
        if (ncstRes == null || !"00".equals(ncstRes.getResultCode())) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "초단기실황 조회 실패");
        }
        Map<String, String> obs = ncstRes.getDataMap();

        // 2) 초단기예보 (폴백 적용)
        KmaResponse fcstRes = null;
        for (String bt : fcstBaseTime(now)) {
            fcstRes = callKma(FCST_PATH, baseDate, bt, coord.getNx(), coord.getNy(), false);
            if ("00".equals(fcstRes.getResultCode()) && fcstRes.getItems().size() > 0) break;
        }
        if (fcstRes == null || !"00".equals(fcstRes.getResultCode())) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "초단기예보 조회 실패");
        }
        Map<String, String> fcst = fcstRes.getDataMap();

        // 상태값 파싱
        String pty = fcst.getOrDefault("PTY", obs.get("PTY"));
        String sky = fcst.get("SKY");
        String condition = resolveCondition(pty, sky);

        String windText = windDirToText(obs.getOrDefault("VEC", null));
        if (windText != null && !windText.endsWith("풍")) windText += "풍";

        return WeatherCardDto.builder()
                .region(region)
                .temperature(parseDouble(obs.get("T1H")))
                .humidity(parseDouble(obs.get("REH")))
                .windSpeed(parseDouble(obs.get("WSD")))
                .windDirection(windText)
                .condition(condition)
                .build();
    }

    /** 실황 기준 baseTime 후보 */
    private String[] ncstBaseTime(LocalDateTime now) {
        int h = now.getHour(), m = now.getMinute();
        String s1 = String.format("%02d00", (m < 40) ? (h + 23) % 24 : h);
        String s2 = String.format("%02d00", (h + 23) % 24);
        return new String[]{s1, s2};
    }

    /** 예보 기준 baseTime 후보 */
    private String[] fcstBaseTime(LocalDateTime now) {
        int h = now.getHour(), m = now.getMinute();
        String s1 = (m >= 45) ? String.format("%02d30", h) : String.format("%02d00", h);
        if (m < 15) s1 = String.format("%02d30", (h + 23) % 24);

        int h2 = s1.endsWith("30") ? h : (h + 23) % 24;
        String s2 = s1.endsWith("30") ? String.format("%02d00", h) : String.format("%02d30", h2);

        int h3 = s2.endsWith("30") ? h2 : (h2 + 23) % 24;
        String s3 = s2.endsWith("30") ? String.format("%02d00", h2) : String.format("%02d30", h3);
        return new String[]{s1, s2, s3};
    }

    /** KMA API 호출 */
    private KmaResponse callKma(String path, String baseDate, String baseTime, int nx, int ny, boolean isNcst) {
        URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromUriString(kmaApiConfig.getBaseUrl())
                .path("/" + path)
                .queryParam("serviceKey", kmaApiConfig.getServiceKey())
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", isNcst ? "200" : "1000")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true).toUri();

        String body = external.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(90))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)).filter(this::isRetryable))
                .block();

        if (body == null || body.isBlank()) {
            log.warn("KMA 빈 응답. URI: {}", uri);
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "KMA 빈 응답");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("KMA JSON 파싱 실패. URI: {}, Body: {}", uri, body, e);
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "KMA 응답 파싱 실패");
        }

        String resultCode = root.path("response").path("header").path("resultCode").asText("99");
        JsonNode items = root.path("response").path("body").path("items").path("item");
        Map<String, String> dataMap = new HashMap<>();

        if ("00".equals(resultCode)) {
            if (isNcst) {
                items.forEach(it -> dataMap.put(it.path("category").asText(), it.path("obsrValue").asText()));
            } else {
                String latestDt = "";
                for (JsonNode it : items) {
                    String dt = it.path("fcstDate").asText() + it.path("fcstTime").asText();
                    if (dt.compareTo(latestDt) > 0) latestDt = dt;
                }
                for (JsonNode it : items) {
                    if ((it.path("fcstDate").asText() + it.path("fcstTime").asText()).equals(latestDt)) {
                        String cat = it.path("category").asText();
                        if ("SKY".equals(cat) || "PTY".equals(cat)) {
                            dataMap.put(cat, it.path("fcstValue").asText());
                        }
                    }
                }
            }
        }
        return new KmaResponse(resultCode, items, dataMap);
    }

    private boolean isRetryable(Throwable t) {
        if (t instanceof WebClientResponseException ex) {
            int s = ex.getRawStatusCode();
            return s == 429 || (s >= 500 && s < 600);
        }
        return t instanceof java.io.IOException
                || t instanceof java.util.concurrent.TimeoutException;
    }

    /** 날씨 조건 문자열 */
    private static String resolveCondition(String pty, String sky) {
        if (pty != null && !"0".equals(pty)) {
            return switch (pty) {
                case "1", "4", "5", "6" -> "비";
                case "2", "3", "7" -> "눈";
                default -> "비";
            };
        }
        return "1".equals(sky) ? "맑음" : "흐림";
    }

    /** 풍향 degree → 텍스트 */
    private String windDirToText(String vec) {
        if (vec == null) return null;
        try {
            double deg = Double.parseDouble(vec);
            String[] dirs = {"북", "북북동", "북동", "동북동", "동", "동남동", "남동", "남남동", "남", "남남서", "남서", "서남서", "서", "서북서", "북서", "북북서"};
            return dirs[(int) Math.round(((deg % 360) / 22.5)) % 16];
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank() || "N/A".equalsIgnoreCase(s)) return null;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }
}
