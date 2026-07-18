package com.example.weather;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import com.example.weather.client.OpenWeatherApiClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final OpenWeatherApiClient openWeatherApiClient;
    private final WeatherRequestRepository weatherRequestRepository;
    private final ObjectMapper objectMapper;

    // These fields are used for testing with WireMock
    String forecastUrl;
    String iconUrl;

    public WeatherService(OpenWeatherApiClient openWeatherApiClient,
                          WeatherRequestRepository weatherRequestRepository) {
        this.openWeatherApiClient = openWeatherApiClient;
        this.weatherRequestRepository = weatherRequestRepository;
        this.objectMapper = new ObjectMapper();
    }

    public ResponseEntity<Map<String, Object>> getWeatherByCityAndDate(String cityName, LocalDate date) {
        log.info("Fetching weather for city: {}, date: {}", cityName, date);

        // Try to find existing request first
        List<WeatherRequest> existingRequests = weatherRequestRepository.findByCityNameAndRequestedDate(cityName, date);
        if (!existingRequests.isEmpty()) {
            WeatherRequest latest = existingRequests.get(0);
            if (latest.getStatus() == WeatherRequest.Status.SUCCESS && latest.getResponsePayload() != null) {
                Map<String, Object> cachedResponse = parseJsonToMap(latest.getResponsePayload());
                return ResponseEntity.ok(cachedResponse);
            }
        }

        // Fetch from OpenWeather API
        String responsePayload;
        try {
            responsePayload = openWeatherApiClient.fetchWeather(cityName, date.toString());
        } catch (Exception e) {
            log.error("Error fetching weather from API", e);
            responsePayload = null;
        }

        // Save to database

        // Save to database
        WeatherRequest weatherRequest = new WeatherRequest();
        weatherRequest.setId(UUID.randomUUID().getMostSignificantBits());
        weatherRequest.setCityName(cityName);
        weatherRequest.setRequestedDate(date);
        weatherRequest.setRequestPayload("{\"city\":\"" + cityName + "\",\"date\":\"" + date + "\"}");

        if (responsePayload != null) {
            weatherRequest.setResponsePayload(responsePayload);
            weatherRequest.setStatus(WeatherRequest.Status.SUCCESS);
        } else {
            weatherRequest.setResponsePayload(null);
            weatherRequest.setStatus(WeatherRequest.Status.ERROR);
        }

        weatherRequest.setCreatedAt(LocalDateTime.now());
        weatherRequestRepository.save(weatherRequest);

        log.info("Weather request saved for city: {} with status: {}", cityName, weatherRequest.getStatus());

        if (responsePayload != null) {
            Map<String, Object> responseMap = parseJsonToMap(responsePayload);
            return ResponseEntity.ok(responseMap);
        }

        return ResponseEntity.internalServerError().build();
    }

    public List<WeatherRequest> findRequestsByCityName(String cityName) {
        return weatherRequestRepository.findByCityName(cityName);
    }

    private Map<String, Object> parseJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse JSON: {}", json, e);
            Map<String, Object> fallback = new java.util.LinkedHashMap<>();
            fallback.put("rawResponse", json);
            fallback.put("status", "ERROR");
            return fallback;
        }
    }
}
