package org.likelionhsu.backend.weather.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WeatherCardsResponse {
    private String city;       // 예: "서산시"
    private String baseDate;   // yyyyMMdd (실황 기준)
    private String baseTime;   // HHmm     (실황 기준)
    private List<WeatherCardDto> cards;
}
