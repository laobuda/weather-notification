package com.example.weather.repository;

import com.example.weather.entity.WeatherRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for WeatherRequest entities.
 */
@Repository
public interface WeatherRequestRepository extends JpaRepository<WeatherRequest, Long> {
    List<WeatherRequest> findByCityNameOrderByCreatedAtDesc(String cityName);
    List<WeatherRequest> findByCityNameAndRequestedDateOrderByCreatedAtDesc(String cityName, LocalDate date);
}
