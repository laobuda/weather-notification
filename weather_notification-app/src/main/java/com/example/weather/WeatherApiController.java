package com.example.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherApiController {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiController.class);

    private final WeatherService weatherService;

    public WeatherApiController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{cityName}/{date}")
    public ResponseEntity<Map<String, Object>> getWeather(
            @PathVariable String cityName,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        log.info("REST API request for city: {}, date: {}", cityName, date);
        return weatherService.getWeatherByCityAndDate(cityName, date);
    }

    @GetMapping("/history/{cityName}")
    public ResponseEntity<List<WeatherRequest>> getHistory(@PathVariable String cityName) {
        log.info("REST API history request for city: {}", cityName);
        List<WeatherRequest> requests = weatherService.findRequestsByCityName(cityName);
        return ResponseEntity.ok(requests);
    }
}
