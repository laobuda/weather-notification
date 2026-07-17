package com.example.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

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
            String message = buildNotificationMessage(cityName, response);
            log.info("Severe weather detected for city: {}. Sending notification.", cityName);
            jmsTemplate.convertAndSend("weather.queue", message);
        } else {
            log.info("Normal weather for city: {}. No notification sent.", cityName);
        }
    }

    private boolean isSevereWeather(org.springframework.http.ResponseEntity<Map<String, Object>> response) {
        if (response == null || response.getBody() == null) {
            return false;
        }

        Map<String, Object> body = response.getBody();
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
        return false;
    }

    private String buildNotificationMessage(String cityName, org.springframework.http.ResponseEntity<Map<String, Object>> response) {
        if (response == null || response.getBody() == null) {
            return String.format("city: %s, status: no data", cityName);
        }

        Map<String, Object> body = response.getBody();
        StringBuilder message = new StringBuilder();
        message.append("city: ").append(cityName);
        message.append(", date: ").append(LocalDate.now());

        Object weatherArray = body.get("weather");
        if (weatherArray instanceof java.util.List<?> weatherList && !weatherList.isEmpty()) {
            Object firstWeather = weatherList.get(0);
            if (firstWeather instanceof Map<?, ?> weatherMap) {
                Object main = weatherMap.get("main");
                if (main != null) {
                    message.append(", severity: ").append(main);
                }
            }
        }

        return message.toString();
    }
}
