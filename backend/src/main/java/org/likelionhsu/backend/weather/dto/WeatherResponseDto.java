package org.likelionhsu.backend.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherResponseDto {

    private String baseDate;        // 발표 일자 (e.g., "20231026")
    private String baseTime;        // 발표 시각 (e.g., "0600")
    private String region;          // 예보 지역
    private String temperature;     // 1시간 기온 (°C)
    private String humidity;        // 습도 (%)
    private String sky;             // 하늘 상태 코드 (맑음(1), 구름많음(3), 흐림(4))
    private String pty;             // 강수 형태 코드 (없음(0), 비(1), 비/눈(2), 눈(3), 소나기(4))
    private String skyDescription;  // 하늘 상태 설명 (맑음, 구름많음, 흐림)
    private String windSpeed;       // 풍속 (m/s)
    private String windDirection;   // 풍향 (deg)

}
