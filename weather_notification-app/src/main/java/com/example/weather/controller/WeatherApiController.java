package com.example.weather.controller;

import com.example.weather.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * REST controller for weather API endpoints.
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherApiController {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiController.class);

    private final WeatherService weatherService;

    public WeatherApiController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Gets weather data for a city and date.
     *
     * @param cityName the city name
     * @param date the date in yyyy-MM-dd format
     * @return weather JSON response
     */
    @GetMapping("/{cityName}/{date}")
    public String getWeather(@PathVariable String cityName, @PathVariable String date) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName must not be empty");
        }
        return weatherService.getWeather(cityName, date);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateTimeParseError(DateTimeParseException ex) {
        log.warn("Invalid date format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid date format. Expected: yyyy-MM-dd", "details", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
