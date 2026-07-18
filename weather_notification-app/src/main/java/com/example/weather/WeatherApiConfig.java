package com.example.weather;

import com.example.weather.client.OpenWeatherApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeatherApiConfig {

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
        return new OpenWeatherApiClient(baseUrl, apiKey);
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
