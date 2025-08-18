package org.likelionhsu.backend.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherCardDto {
    private String region;          // 지역명 (해미면 등)
    private Double temperature;     // ℃
    private Double humidity;        // %
    private Double windSpeed;       // m/s
    private String windDirection;   // 예: "남서풍"
    private String condition;       // "맑음/흐림/비/눈"
}
