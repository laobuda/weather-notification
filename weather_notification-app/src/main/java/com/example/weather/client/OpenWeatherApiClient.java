package com.example.weather.client;

import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class OpenWeatherApiClient {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    public String fetchWeather(String cityName, String date) throws Exception {
        // In a real scenario, 'date' would be part of query params or path
        // For this implementation, we simulate the call to the external API
        URL url = new URL(BASE_URL + "?q=" + cityName + "&appid=dummy_key");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else if (responseCode == 404) {
            return null;
        } else {
            throw new RuntimeException("HTTP error code: " + responseCode);
        }
    }

    // Overload for testing/wiremock purposes where we might want to pass a custom URL
    public String fetchWeatherFromUrl(String targetUrl) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else if (responseCode == 404) {
            return null;
        } else {
            throw new RuntimeException("HTTP error code: " + responseCode);
        }
    }
}
