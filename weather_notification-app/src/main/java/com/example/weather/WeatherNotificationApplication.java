package com.example.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Weather Notification Service.
 * 
 * This Spring Boot service consumes messages from an Apache Artemis queue,
 * exposes a REST API to query historical/current weather by city and date,
 * and persists successful requests/responses to PostgreSQL.
 */
@SpringBootApplication
public class WeatherNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherNotificationApplication.class, args);
    }
}
