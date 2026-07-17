package com.example.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class OpenWeatherApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherApiClient.class);

    private final Environment environment;

    public OpenWeatherApiClient(Environment environment) {
        this.environment = environment;
    }

    public String fetchWeather(String cityName) {
        try {
            String baseUrl = environment.getProperty("weather.api.base-url", "https://api.openweathermap.org/data/2.5/weather");
            String apiKey = environment.getProperty("weather.api.api-key", "dummy-key");
            String url = baseUrl + "?appid=" + apiKey + "&q=" + cityName;
            log.info("Calling OpenWeather API: {}", url);

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            log.info("OpenWeather API response code: {}", responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();
                String body = response.toString();
                log.info("Successfully received weather data for city");
                return body;
            } else if (responseCode == 404) {
                log.warn("City not found: {}", cityName);
                connection.disconnect();
                return null;
            } else {
                log.error("Error from OpenWeather API: {}", responseCode);
                connection.disconnect();
                return null;
            }
        } catch (Exception e) {
            log.error("Exception calling OpenWeather API", e);
            return null;
        }
    }
}
