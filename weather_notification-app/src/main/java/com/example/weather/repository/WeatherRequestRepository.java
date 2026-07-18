package com.example.weather.repository;

import com.example.weather.entity.WeatherRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherRequestRepository extends JpaRepository<WeatherRequest, Long> {
}
