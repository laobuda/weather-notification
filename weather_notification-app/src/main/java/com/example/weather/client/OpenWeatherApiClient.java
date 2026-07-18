package com.example.weather.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OpenWeatherApiClient {

    private final String baseUrl;
    private final String apiKey;

    public OpenWeatherApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public String fetchWeather(String cityName, String date) throws Exception {
        URL url = new URL(baseUrl + "?q=" + cityName + "&appid=" + apiKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
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
        } finally {
            connection.disconnect();
        }
    }

    public String fetchForecast(String forecastUrl) throws Exception {
        URL url = new URL(forecastUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
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
        } finally {
            connection.disconnect();
        }
    }
}
