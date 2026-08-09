package com.example.weather.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client for OpenWeatherMap API.
 */
public class OpenWeatherApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherApiClient.class);

    private final String baseUrl;
    private final String apiKey;

    public OpenWeatherApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Fetches current weather data for a city.
     *
     * @param cityName the city name
     * @param date the date (used for logging/caching purposes)
     * @return JSON response from the API, or null if not found
     * @throws Exception if the request fails
     */
    public String fetchWeather(String cityName, String date) throws Exception {
        String query = "q=" + java.net.URLEncoder.encode(cityName, StandardCharsets.UTF_8)
                + "&appid=" + apiKey;
        URL url = URI.create(baseUrl + "?" + query).toURL();
        return executeRequest(url, "weather");
    }

    /**
     * Fetches forecast data from a given URL.
     *
     * @param forecastUrl the forecast URL
     * @return JSON response from the API, or null if not found
     * @throws Exception if the request fails
     */
    public String fetchForecast(String forecastUrl) throws Exception {
        URL url = new URL(forecastUrl);
        return executeRequest(url, "forecast");
    }

    /**
     * Executes an HTTP GET request and handles the response.
     *
     * @param url the URL to request
     * @param requestType description for logging
     * @return the response body, or null if 404
     * @throws Exception if the request fails with an error status
     */
    private String executeRequest(URL url, String requestType) throws Exception {
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
                log.debug("{} request returned 404 for URL: {}", requestType, url);
                return null;
            } else {
                // Consume error stream to prevent connection leak
                try (var errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        errorStream.transferTo(java.io.OutputStream.nullOutputStream());
                    }
                }
                throw new RuntimeException("HTTP error code: " + responseCode + " for " + requestType + " request");
            }
        } finally {
            connection.disconnect();
        }
    }
}
