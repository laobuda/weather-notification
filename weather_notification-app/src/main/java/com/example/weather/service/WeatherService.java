package com.example.weather.service;

import com.example.weather.client.OpenWeatherApiClient;
import com.example.weather.entity.WeatherRequest;
import com.example.weather.repository.WeatherRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WeatherService {

    private final OpenWeatherApiClient apiClient;
    private final WeatherRequestRepository repository;

    public WeatherService(OpenWeatherApiClient apiClient, WeatherRequestRepository repository) {
        this.apiClient = apiClient;
        this.repository = repository;
    }

    @Transactional
    public String getWeather(String cityName, String date) {
        WeatherRequest request = new WeatherRequest();
        request.setCityName(cityName);
        request.setRequestedDate(date);
        request.setRequestPayload("{\"cityName\":\"" + cityName + "\", \"date\":\"" + date + "\"}");
        request.setCreatedAt(LocalDateTime.now());

        try {
            String response = apiClient.fetchWeather(cityName, date);
            if (response != null) {
                request.setResponsePayload(response);
                request.setStatus(WeatherRequest.Status.SUCCESS);
            } else {
                request.setResponsePayload(null);
                request.setStatus(WeatherRequest.Status.NOT_FOUND);
            }
        } catch (Exception e) {
            request.setResponsePayload(e.getMessage());
            request.setStatus(WeatherRequest.Status.ERROR);
        }

        repository.save(request);
        return request.getResponsePayload();
    }

    @Transactional
    public void processNotification(String message) {
        // Logic for processing JMS message
        // For simulation, we just log the arrival of a message
        System.out.println("Processing notification from queue: " + message);
    }
}
