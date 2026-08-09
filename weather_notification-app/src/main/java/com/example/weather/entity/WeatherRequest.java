package com.example.weather.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_requests")
public class WeatherRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false)
    private LocalDate requestedDate;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum Status {
        SUCCESS, NOT_FOUND, ERROR
    }

    // Private constructor for Builder
    private WeatherRequest() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }
    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }
    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Builder pattern for creating WeatherRequest instances.
     */
    public static class Builder {
        private String cityName;
        private LocalDate requestedDate;
        private String requestPayload;
        private String responsePayload;
        private Status status;
        private LocalDateTime createdAt;

        public Builder withCityName(String cityName) {
            this.cityName = cityName;
            return this;
        }

        public Builder withRequestedDate(LocalDate requestedDate) {
            this.requestedDate = requestedDate;
            return this;
        }

        public Builder withRequestPayload(String requestPayload) {
            this.requestPayload = requestPayload;
            return this;
        }

        public Builder withResponsePayload(String responsePayload) {
            this.responsePayload = responsePayload;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder withCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public WeatherRequest build() {
            WeatherRequest request = new WeatherRequest();
            request.cityName = this.cityName;
            request.requestedDate = this.requestedDate;
            request.requestPayload = this.requestPayload;
            request.responsePayload = this.responsePayload;
            request.status = this.status != null ? this.status : Status.ERROR;
            request.createdAt = this.createdAt != null ? this.createdAt : LocalDateTime.now();
            return request;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
