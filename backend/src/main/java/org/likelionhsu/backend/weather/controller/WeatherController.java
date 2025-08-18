package org.likelionhsu.backend.weather.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.weather.dto.WeatherCardsResponse;
import org.likelionhsu.backend.weather.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private final WeatherService weatherService;

    /** 카드형 날씨(여러 지역 한 번에) */
    @GetMapping("/cards")
    public ResponseEntity<WeatherCardsResponse> getCards(@RequestParam String city) {
        return ResponseEntity.ok(weatherService.getCardsByCity(city));
    }
}
