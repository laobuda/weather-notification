package com.example.weather;

import com.example.weather.client.OpenWeatherApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenWeatherMap API client.
 */
@Configuration
public class WeatherApiConfig {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiConfig.class);
    private static final String DEFAULT_WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";

    @Value("${weather.api.base-url:}")
    private String baseUrl;

    @Value("${weather.api.api-key:dummy-key}")
    private String apiKey;

    @Value("${weather.api.forecast-url:}")
    private String forecastUrl;

    @Value("${weather.api.icon-url:}")
    private String iconUrl;

    @Bean
    public OpenWeatherApiClient openWeatherApiClient() {
        String effectiveUrl = baseUrl;
        if (effectiveUrl.isBlank()) {
            log.warn("weather.api.base-url is not configured. Using default: {}", DEFAULT_WEATHER_API_URL);
            effectiveUrl = DEFAULT_WEATHER_API_URL;
        }
        return new OpenWeatherApiClient(effectiveUrl, apiKey);
    }

    @Bean
    public String forecastUrl() {
        return forecastUrl;
    }

    @Bean
    public String iconUrl() {
        return iconUrl;
    }
}
