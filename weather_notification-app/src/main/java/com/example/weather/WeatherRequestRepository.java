package com.example.weather;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WeatherRequestRepository extends JpaRepository<WeatherRequest, Long> {

    List<WeatherRequest> findByCityName(String cityName);

    List<WeatherRequest> findByCityNameAndRequestedDate(String cityName, LocalDate requestedDate);
}
