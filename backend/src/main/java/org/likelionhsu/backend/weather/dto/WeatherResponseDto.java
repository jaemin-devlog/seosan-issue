package org.likelionhsu.backend.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherResponseDto {

    private String baseDate;
    private String baseTime;
    private String region;
    private String temperature;
    private String humidity;
    private String sky;
    private String pty;
    private String skyDescription; // ✅ 여기가 없으면 오류 발생
    private String windSpeed;
    private String windDirection;

}
