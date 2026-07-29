package com.example.weather.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
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
        String query = "q=" + java.net.URLEncoder.encode(cityName, StandardCharsets.UTF_8)
                + "&appid=" + apiKey;
        URL url = URI.create(baseUrl + "?" + query).toURL();
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
                // Consume error stream to prevent connection leak
                try (var errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        errorStream.transferTo(java.io.OutputStream.nullOutputStream());
                    }
                }
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
                // Consume error stream to prevent connection leak
                try (var errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        errorStream.transferTo(java.io.OutputStream.nullOutputStream());
                    }
                }
                throw new RuntimeException("HTTP error code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
}
