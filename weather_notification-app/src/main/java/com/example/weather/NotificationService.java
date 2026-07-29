package com.example.weather;

import com.example.weather.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    private final WeatherService weatherService;
    private final JmsTemplate jmsTemplate;

    public NotificationService(WeatherService weatherService, JmsTemplate jmsTemplate) {
        this.weatherService = weatherService;
        this.jmsTemplate = jmsTemplate;
    }

    public void processWeatherRequest(String cityName, LocalDate date) {
        log.info("Processing weather request for city: {}, date: {}", cityName, date);

        // Step 1: Fetch weather data
        var response = weatherService.getWeatherByCityAndDate(cityName, date);

        // Step 2: Check if weather is severe
        if (isSevereWeather(response)) {
            // Step 3: Send notification to JMS queue
            String message = buildNotificationMessage(cityName, date, response);
            log.info("Severe weather detected for city: {}. Sending notification.", cityName);
            jmsTemplate.convertAndSend("weather.queue", message);
        } else {
            log.info("Normal weather for city: {}. No notification sent.", cityName);
        }
    }

    private boolean isSevereWeather(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        try {
            Map<String, Object> body = MAPPER.readValue(
                    response,
                    new TypeReference<Map<String, Object>>() {});

            Object weatherArray = body.get("weather");
            if (weatherArray instanceof java.util.List<?> weatherList) {
                for (Object weather : weatherList) {
                    if (weather instanceof Map<?, ?> weatherMap) {
                        Object main = weatherMap.get("main");
                        if (main != null && "severe".equalsIgnoreCase(main.toString())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing weather response", e);
        }
        return false;
    }

    private String buildNotificationMessage(String cityName, LocalDate date, String response) {
        try {
            Map<String, Object> body = (response != null && !response.isEmpty())
                    ? MAPPER.readValue(response, new TypeReference<Map<String, Object>>() {})
                    : Map.of();

            String severity = null;
            Object weatherArray = body.get("weather");
            if (weatherArray instanceof java.util.List<?> weatherList && !weatherList.isEmpty()) {
                Object firstWeather = weatherList.get(0);
                if (firstWeather instanceof Map<?, ?> weatherMap) {
                    severity = (String) weatherMap.get("main");
                }
            }

            return buildJsonMessage(cityName, date, severity);
        } catch (Exception e) {
            log.error("Error parsing weather response for notification", e);
            return buildJsonMessage(cityName, date, "error");
        }
    }

    private String buildJsonMessage(String cityName, LocalDate date, String severity) {
        return MAPPER.createObjectNode()
                .put("city", cityName)
                .put("date", date.toString())
                .put("severity", severity)
                .toString();
    }
}
