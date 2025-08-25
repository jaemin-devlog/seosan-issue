package org.likelionhsu.backend.weather.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.common.config.KmaApiConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/weather")
public class FinalWeatherController {

    private final WeatherApiDelegate weatherApiDelegate;

    @GetMapping("/{kind}")
    public Mono<Map<String, Object>> getWeatherData(
            @PathVariable String kind,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String base_date,
            @RequestParam(required = false) String base_time
    ) {
        kind = kind.toLowerCase(Locale.ROOT);

        if (city != null && !city.isBlank()) {
            return weatherApiDelegate.getWeatherForCity(kind, city, base_date, base_time);
        }
        if (region != null && !region.isBlank()) {
            return weatherApiDelegate.getWeatherForRegion(kind, region, base_date, base_time)
                    .map(result -> Map.of("result", result));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "city 또는 region 파라미터 중 하나는 필수입니다.");
    }
}

@Component
@Slf4j
class WeatherApiDelegate {

    private final WebClient web;
    private final KmaApiConfig kma;
    private final ObjectMapper mapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public WeatherApiDelegate(@Qualifier("externalWebClient") WebClient web, KmaApiConfig kma, ObjectMapper mapper) {
        this.web = web;
        this.kma = kma;
        this.mapper = mapper;
    }

    public Mono<Map<String, Object>> getWeatherForCity(String kind, String city, String baseDate, String baseTime) {
        String processedCityKey = city.trim();

        // ★★★ 수정 지점: 한글 도시 이름을 yml의 영문 키(seosan)로 매핑 ★★★
        if ("서산시".equals(processedCityKey) || "서산시 전체".equals(processedCityKey)) {
            processedCityKey = "seosan";
        }

        List<String> regions = kma.regionsOfCity(processedCityKey);
        if (regions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 도시 그룹입니다: " + city.trim());
        }

        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < regions.size(); i++) {
            orderMap.put(regions.get(i), i);
        }

        return Flux.fromIterable(regions)
                .flatMap(region -> callKmaAndMapIcon(kind, region, baseDate, baseTime), 6)
                .collectList()
                .map(results -> {
                    results.sort(Comparator.comparingInt(r -> orderMap.getOrDefault((String) r.get("region"), Integer.MAX_VALUE)));
                    return Map.of(
                            "city", city.trim(), // 응답에는 원래 요청한 한글 도시 이름을 보여줌
                            "count", results.size(),
                            "results", results
                    );
                });
    }

    public Mono<Map<String, Object>> getWeatherForRegion(String kind, String region, String baseDate, String baseTime) {
        return callKmaAndMapIcon(kind, region.trim(), baseDate, baseTime);
    }

    // ... (이하 다른 메서드들은 이전과 동일)
    private Mono<Map<String, Object>> callKmaAndMapIcon(String kind, String region, String baseDate, String baseTime) {
        var rc = kma.findByName(region)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 지역입니다: " + region));

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        boolean isTimeAutoCalculated = false;
        if (!StringUtils.hasText(baseDate)) {
            baseDate = now.format(DATE_FORMATTER);
            isTimeAutoCalculated = true;
        }
        if (!StringUtils.hasText(baseTime)) {
            baseTime = "ncst".equals(kind) ? getBaseTimeForNcst(now) : getBaseTimeForFcst(now);
            isTimeAutoCalculated = true;
        }

        if (isTimeAutoCalculated && now.getHour() == 0 && "2300".equals(baseTime)) {
            baseDate = now.minusDays(1).format(DATE_FORMATTER);
        }

        String endpoint = switch (kind) {
            case "ncst" -> "/getUltraSrtNcst";
            case "fcst" -> "/getUltraSrtFcst";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind는 'ncst' 또는 'fcst'여야 합니다.");
        };

        final String requestDate = baseDate;
        final String requestTime = baseTime;

        URI uri = UriComponentsBuilder.fromHttpUrl(kma.getBaseUrl())
                .path(endpoint)
                .queryParam("serviceKey", kma.getServiceKey())
                .queryParam("dataType", "JSON")
                .queryParam("numOfRows", 500) // ← 안전하게 500으로 줄임
                .queryParam("pageNo", 1)
                .queryParam("base_date", requestDate)
                .queryParam("base_time", requestTime)
                .queryParam("nx", rc.getNx())
                .queryParam("ny", rc.getNy())
                .build(false)   // ★ 인코딩 방지 (이중 인코딩 막기)
                .toUri();

        return web.get().uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> new ResponseStatusException(resp.statusCode(),
                                        "KMA HTTP " + resp.statusCode().value() + " " + body))
                )
                .bodyToMono(String.class)
                .map(body -> {
                    if (body.trim().startsWith("<")) {   // ★ HTML fallback 처리
                        log.error("KMA returned HTML instead of JSON: {}",
                                body.substring(0, Math.min(200, body.length())));
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "KMA 비JSON 응답 (HTML)");
                    }
                    return parseAndMapIcon(body);
                })
                .map(iconInfo -> {
                    Map<String, Object> result = new LinkedHashMap<>(iconInfo);
                    result.put("region", region);
                    result.put("baseDate", requestDate);
                    result.put("baseTime", requestTime);
                    result.put("nx", rc.getNx());
                    result.put("ny", rc.getNy());
                    return result;
                })
                .onErrorResume(e -> {
                    log.error("날씨 조회 실패 (region: {}): {}", region, e.getMessage());
                    return Mono.just(Map.of("region", region, "error", "데이터를 가져올 수 없습니다."));
                });
    }


    private Map<String, Object> parseAndMapIcon(@NonNull String json) {
        try {
            JsonNode root = mapper.readTree(json);

            if (!"00".equals(root.at("/response/header/resultCode").asText(""))) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "KMA Error: " + root.at("/response/header/resultMsg").asText("Unknown Error")
                );
            }

            JsonNode items = root.at("/response/body/items/item");
            if (!items.isArray() || items.size() == 0) {
                return Map.of("condition", "맑음", "conditionCode", "sunny");
            }

            boolean isFcst = items.get(0).has("fcstDate");
            String targetDate = null, targetTime = null;

            if (isFcst) {
                String minKey = null;
                for (JsonNode it : items) {
                    String d = it.path("fcstDate").asText(null);
                    String t = it.path("fcstTime").asText(null);
                    if (d == null || t == null) continue;
                    String k = d + t;
                    if (minKey == null || k.compareTo(minKey) < 0) {
                        minKey = k;
                        targetDate = d;
                        targetTime = t;
                    }
                }
            }

            String pty = null, sky = null, rn1 = null, t1h = null;
            String reh = null, wsd = null, vec = null;

            for (JsonNode it : items) {
                String cat = it.path("category").asText();
                String val = it.has("obsrValue") ? it.path("obsrValue").asText() : it.path("fcstValue").asText();

                if (isFcst) {
                    String d = it.path("fcstDate").asText(null);
                    String t = it.path("fcstTime").asText(null);
                    if (!Objects.equals(targetDate, d) || !Objects.equals(targetTime, t)) continue;
                }
                switch (cat) {
                    case "PTY" -> pty = val;
                    case "SKY" -> sky = val;
                    case "RN1" -> rn1 = val;
                    case "T1H" -> t1h = val;
                    case "REH" -> reh = val;
                    case "WSD" -> wsd = val;
                    case "VEC" -> vec = val;
                }
            }

            String condition = mapIcon(pty, sky, rn1, t1h);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("condition", condition);
            result.put("conditionCode", toConditionCode(condition));
            if (t1h != null) result.put("temperature", parseDouble(t1h));
            if (reh != null) result.put("humidity", parseDouble(reh));
            if (wsd != null) result.put("windSpeed", parseDouble(wsd));
            if (vec != null) result.put("windDirection", windDirToText(vec));

            return result;

        } catch (Exception e) {
            log.error("KMA 응답 파싱 실패", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "KMA 응답 처리 중 오류가 발생했습니다.");
        }
    }

    private static String mapIcon(String pty, String sky, String rn1, String t1h) {
        if (pty != null && !"0".equals(pty)) {
            return ("1".equals(pty) || "4".equals(pty) || "5".equals(pty)) ? "비" : "눈";
        }
        if (sky != null) return "1".equals(sky) ? "맑음" : "흐림";
        if (rn1 != null && parseNumberOrZero(rn1) > 0) return "비";
        return "맑음";
    }

    private static String toConditionCode(String condition) {
        return switch (condition) {
            case "맑음" -> "sunny";
            case "흐림" -> "cloudy";
            case "비" -> "rain";
            default -> "snow";
        };
    }

    private static double parseNumberOrZero(String s) {
        if (s == null) return 0.0;
        try {
            return Double.parseDouble(s.replaceAll("[^0-9.\\-]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String windDirToText(String vec) {
        if (vec == null) return null;
        try {
            double deg = Double.parseDouble(vec);
            String[] dirs = {"북", "북북동", "북동", "동북동", "동", "동남동", "남동", "남남동", "남", "남남서", "남서", "서남서", "서", "서북서", "북서", "북북서"};
            return dirs[(int) Math.round(((deg % 360) / 22.5)) % 16] + "풍";
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

    private String getBaseTimeForNcst(LocalDateTime now) {
        int hour = now.getHour();
        if (now.getMinute() < 40) {
            hour -= 1;
        }
        if (hour < 0) hour = 23;
        return String.format("%02d00", hour);
    }

    private String getBaseTimeForFcst(LocalDateTime now) {
        int hour = now.getHour();
        if (now.getMinute() < 45) {
            hour -= 1;
        }
        if (hour < 0) hour = 23;
        return String.format("%02d00", hour);
    }
}
