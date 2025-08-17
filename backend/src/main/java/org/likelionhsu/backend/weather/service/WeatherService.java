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

    private static final String NCST_PATH = "getUltraSrtNcst";  // 초단기실황
    private static final String FCST_PATH = "getUltraSrtFcst";  // 초단기예보

    public WeatherResponseDto getWeather(String region) {
        KmaApiConfig.RegionCoordinate coord = kmaApiConfig.getGridCoords().stream()
                .filter(c -> c.getName().equals(region))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 지역: " + region));

        LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime();

        // 1) 실황: 관측값(기온/습도/풍속/풍향/PTY)
        String baseDateNcst = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTimeNcst = ncstBaseTime(now);
        Map<String, String> obs = callKma(NCST_PATH, baseDateNcst, baseTimeNcst, coord.getNx(), coord.getNy(), true);

        // 2) 예보: SKY/PTY 보강(가장 최근 fcstTime)
        String baseDateFcst = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTimeFcst = fcstBaseTime(now);
        Map<String, String> fcst = callKma(FCST_PATH, baseDateFcst, baseTimeFcst, coord.getNx(), coord.getNy(), false);

        String sky = fcst.getOrDefault("SKY", "N/A");
        String pty = fcst.getOrDefault("PTY", obs.getOrDefault("PTY", "N/A"));

        String windText = windDirToText(obs.getOrDefault("VEC", null));

        return WeatherResponseDto.builder()
                .baseDate(baseDateNcst)
                .baseTime(baseTimeNcst)
                .region(region)
                .temperature(obs.getOrDefault("T1H", "N/A"))
                .humidity(obs.getOrDefault("REH", "N/A"))
                .sky(sky)
                .pty(pty)
                .skyDescription(describe(sky, pty))
                .windSpeed(obs.getOrDefault("WSD", "N/A"))
                .windDirection(windText == null ? obs.getOrDefault("VEC", "N/A") : windText)
                .build();
    }

    // ----- Helpers -----

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
     * 공통 호출: isNcst=true → obsrValue, false → fcstValue(SKY/PTY 최신 fcstTime만)
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
                .build(true) // 이미 인코딩된 키 사용
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        JsonNode root;
        try { root = objectMapper.readTree(res.getBody()); }
        catch (Exception e) { throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "KMA 응답 파싱 실패"); }

        String code = root.path("response").path("header").path("resultCode").asText();
        if (!"00".equals(code)) {
            String msg = root.path("response").path("header").path("resultMsg").asText();
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "KMA 호출 실패: " + msg);
        }

        JsonNode items = root.path("response").path("body").path("items").path("item");
        Map<String, String> out = new HashMap<>();

        if (isNcst) {
            // 관측값 전부 맵핑
            for (JsonNode it : items) {
                out.put(it.path("category").asText(), it.path("obsrValue").asText());
            }
            return out;
        }

        // 예보: 가장 최신 fcstDate+fcstTime 구하기
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

    /** PTY 우선 → SKY 보조 */
    private String describe(String skyCode, String ptyCode) {
        switch (ptyCode) {
            case "1": return "비";
            case "2": return "비/눈";
            case "3": return "눈";
            case "4": return "소나기";
            case "5": return "빗방울";
            case "6": return "빗방울·눈날림";
            case "7": return "눈날림";
        }
        switch (skyCode) {
            case "1": return "맑음";
            case "3": return "구름많음";
            case "4": return "흐림";
            default:  return "정보 없음";
        }
    }

    /** 풍향각(0~360) → 16방위 텍스트 */
    private String windDirToText(String vec) {
        if (vec == null) return null;
        try {
            double deg = Double.parseDouble(vec);
            String[] dirs = {"북","북북동","북동","동북동","동","동남동","남동","남남동","남","남남서","남서","서남서","서","서북서","북서","북북서"};
            int idx = (int)Math.round(((deg % 360) / 22.5)) % 16;
            return dirs[idx];
        } catch (NumberFormatException e) { return null; }
    }
}
