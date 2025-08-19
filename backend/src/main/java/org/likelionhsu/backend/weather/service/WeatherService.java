package org.likelionhsu.backend.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.common.config.KmaApiConfig;
import org.likelionhsu.backend.common.config.KmaApiConfig.RegionCoordinate;
import org.likelionhsu.backend.common.exception.CustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.weather.dto.WeatherCardDto;
import org.likelionhsu.backend.weather.dto.WeatherCardsResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KmaApiConfig kmaApiConfig;

    // KMA 엔드포인트(베이스 URL은 KmaApiConfig에 설정)
    private static final String NCST_PATH = "getUltraSrtNcst";  // 초단기실황 (관측)
    private static final String FCST_PATH = "getUltraSrtFcst";  // 초단기예보 (SKY/PTY)

    /** 카드형 날씨: city는 메타로만 사용, gridCoords 전체를 순회해 카드 생성 */
    public WeatherCardsResponse getCardsByCity(String city) {
        var coords = Optional.ofNullable(kmaApiConfig.getGridCoords())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE, "grid-coords가 비어 있습니다."));

        LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime();
        String baseDateNcst = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTimeNcst = ncstBaseTime(now);

        var pool = Executors.newFixedThreadPool(Math.min(6, coords.size()));
        try {
            var futures = coords.stream()
                    .map(rc -> CompletableFuture.supplyAsync(
                            () -> buildCard(rc.getName(), baseDateNcst, baseTimeNcst), pool))
                    .toList();

            List<WeatherCardDto> cards = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

            return WeatherCardsResponse.builder()
                    .city(city)                 // 전달받은 city 그대로 표기
                    .baseDate(baseDateNcst)    // 실황 기준
                    .baseTime(baseTimeNcst)
                    .cards(cards)
                    .build();
        } finally {
            pool.shutdown();
        }
    }

    /** 카드 한 장 생성 (실황 + 최신 SKY/PTY → Figma 4종으로 축약) */
    private WeatherCardDto buildCard(String region, String baseDateNcst, String baseTimeNcst) {
        RegionCoordinate coord = kmaApiConfig.getGridCoords().stream()
                .filter(c -> c.getName().equals(region))
                .findFirst()
                .orElse(null);
        if (coord == null) return null;

        // 1) 초단기실황(관측)
        Map<String, String> obs = callKma(NCST_PATH, baseDateNcst, baseTimeNcst, coord.getNx(), coord.getNy(), true);
        Double t1h = parseDouble(obs.get("T1H"));
        Double reh = parseDouble(obs.get("REH"));
        Double wsd = parseDouble(obs.get("WSD"));
        String windText = windDirToText(obs.getOrDefault("VEC", null)); // "남서"
        if (windText != null && !windText.endsWith("풍")) windText += "풍"; // "남서풍"

        // 2) 초단기예보(가장 최신 SKY/PTY)
        LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime();
        String baseDateFcst = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTimeFcst = fcstBaseTime(now);
        Map<String, String> fcst = callKma(FCST_PATH, baseDateFcst, baseTimeFcst, coord.getNx(), coord.getNy(), false);

        String sky = fcst.get("SKY");
        String pty = fcst.getOrDefault("PTY", obs.get("PTY")); // 예보 비어있으면 실황 PTY 보조
        String condition = resolveCondition(pty, sky); // "맑음/흐림/비/눈"

        return WeatherCardDto.builder()
                .region(region)
                .temperature(t1h)
                .humidity(reh)
                .windSpeed(wsd)
                .windDirection(windText)
                .condition(condition)
                .build();
    }

    // ====== 공통 유틸 ======

    /** 실황 base_time: 매시 정각, 10분 이후 제공 → 10분 전이면 이전 시각 00 */
    private String ncstBaseTime(LocalDateTime now) {
        int h = (now.getMinute() < 10) ? now.minusHours(1).getHour() : now.getHour();
        if (h < 0) h = 23;
        return String.format("%02d00", h);
    }

    /** 예보 base_time: 00/30분 슬롯. 40분 전까지는 직전 슬롯, 40분 이후는 HH30 */
    private String fcstBaseTime(LocalDateTime now) {
        int m = now.getMinute(), h = now.getHour();
        if (m < 10) return String.format("%02d30", (h + 23) % 24);  // 00~09 → 이전시각 30
        if (m < 40) return String.format("%02d00", h);              // 10~39 → HH00
        return String.format("%02d30", h);                          // 40~59 → HH30
    }

    /**
     * KMA 공통 호출
     * isNcst=true  → obsrValue (실황)
     * isNcst=false → fcstValue (예보: 최신 fcstDate+fcstTime 중 SKY/PTY만 추출)
     */
        private Map<String, String> callKma(String path, String baseDate, String baseTime,
                                        int nx, int ny, boolean isNcst) {
        URI uri = UriComponentsBuilder.fromUriString(kmaApiConfig.getBaseUrl() + "/" + path)
                .queryParam("serviceKey", kmaApiConfig.getServiceKey())
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "200")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        log.debug("KMA API URI: {}", uri);
        log.debug("KMA API Response: {}", res.getBody());

        JsonNode root;
        try {
            if (res.getBody() == null || !res.getBody().trim().startsWith("{")) {
                log.error("KMA 응답이 비어있거나 JSON 형식이 아닙니다. Body: {}", res.getBody());
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "KMA 응답이 비어있거나 JSON 형식이 아닙니다.");
            }
            root = objectMapper.readTree(res.getBody());
        } catch (Exception e) {
            log.error("KMA 응답 파싱 실패. Body: {}", res.getBody(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "KMA 응답 파싱 실패");
        }

        String code = root.path("response").path("header").path("resultCode").asText();
        if (!"00".equals(code)) {
            String msg = root.path("response").path("header").path("resultMsg").asText();
            log.error("KMA API가 에러 코드를 반환했습니다. Code: {}, Msg: {}", code, msg);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "KMA 호출 실패: " + msg);
        }

        JsonNode items = root.path("response").path("body").path("items").path("item");
        Map<String, String> out = new HashMap<>();

        if (isNcst) {
            for (JsonNode it : items) {
                out.put(it.path("category").asText(), it.path("obsrValue").asText());
            }
            return out;
        }

        String latestDt = "";
        for (JsonNode it : items) {
            String dt = it.path("fcstDate").asText() + it.path("fcstTime").asText();
            if (dt.compareTo(latestDt) > 0) latestDt = dt;
        }
        for (JsonNode it : items) {
            if (!(it.path("fcstDate").asText() + it.path("fcstTime").asText()).equals(latestDt)) continue;
            String cat = it.path("category").asText();
            if ("SKY".equals(cat) || "PTY".equals(cat)) {
                out.put(cat, it.path("fcstValue").asText());
            }
        }
        return out;
    }

    /** PTY 우선 → SKY 보조 (Figma 4종으로 축약) */
    private static String resolveCondition(String pty, String sky) {
        if (pty != null && !"0".equals(pty)) {
            return switch (pty) {
                case "1", "4", "5", "6" -> "비";   // 비/소나기/빗방울
                case "2", "3", "7" -> "눈";       // 비눈/눈/눈날림
                default -> "비";
            };
        }
        if ("1".equals(sky)) return "맑음";
        return "흐림"; // SKY=3,4 나머지는 전부 흐림 처리
    }

    /** 풍향각(0~360) → 16방위 텍스트(한글 두 글자) */
    private String windDirToText(String vec) {
        if (vec == null) return null;
        try {
            double deg = Double.parseDouble(vec);
            String[] dirs = {"북","북북동","북동","동북동","동","동남동","남동","남남동","남","남남서","남서","서남서","서","서북서","북서","북북서"};
            int idx = (int)Math.round(((deg % 360) / 22.5)) % 16;
            return dirs[idx];
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank() || "N/A".equalsIgnoreCase(s)) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }
}
