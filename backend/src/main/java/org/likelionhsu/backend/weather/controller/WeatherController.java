package org.likelionhsu.backend.weather.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.weather.dto.WeatherResponseDto;
import org.likelionhsu.backend.weather.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<WeatherResponseDto> getWeather(@RequestParam String region) {
        WeatherResponseDto weather = weatherService.getWeather(region);
        return ResponseEntity.ok(weather);
    }
}
