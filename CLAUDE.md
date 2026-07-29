# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Weather Notification Service — a Spring Boot 4.1.0 application (Java 21) that fetches weather data from the OpenWeatherMap API, persists requests to PostgreSQL, and sends JMS notifications when severe weather is detected.

## Build & Run

```bash
# From project root
mvn clean package                              # Build the project
mvn -pl weather_notification-app spring-boot:run # Run the application (default port 9090)
```

## Testing

```bash
mvn test                                        # Unit tests
mvn verify -pl weather_notification-app         # Integration tests (failsafe plugin)
mvn verify -pl weather_notification-app -Dtest=NotificationFlowIT   # Single test class
mvn verify -pl weather_notification-app -Dtest=WeatherApiFlowIT    # Single test class
```

Integration tests use Testcontainers for PostgreSQL, ActiveMQ Artemis, and WireMock for the external weather API.

## Architecture

### Module Structure

Single module: `weather_notification-app` (Spring Boot 4.1.0 parent, pom `packaging`)

### Key Components

| Layer | Class | Responsibility |
|-------|-------|----------------|
| REST Controller | `WeatherApiController` | `GET /api/weather/{cityName}/{date}` — accepts weather queries |
| Service | `WeatherService` | Orchestrates API calls, maps responses, persists `WeatherRequest` entities |
| Service | `NotificationService` | Checks for severe weather and publishes to JMS queue `weather.queue` |
| Client | `OpenWeatherApiClient` | Low-level HTTP calls to OpenWeatherMap (raw JSON strings) |
| Entity | `WeatherRequest` | JPA entity stored in `weather_requests` table (columns: id, cityName, requestedDate, requestPayload, responsePayload, status enum, createdAt) |
| Repository | `WeatherRequestRepository` | Spring Data JPA — `findByCityNameOrderByCreatedAtDesc`, `findByCityNameAndRequestedDateOrderByCreatedAtDesc` |
| Config | `JmsConfig` | Artemis connection factory, JMS listener container, JMS messaging template |
| Config | `WeatherApiConfig` | OpenWeatherApiClient bean + forecast/icon URL beans |

### Data Flow

1. Client calls `GET /api/weather/{city}/{date}`
2. `WeatherService` calls `OpenWeatherApiClient` for current weather and forecast (raw JSON strings)
3. Response is persisted as `WeatherRequest` (SUCCESS / NOT_FOUND / ERROR)
4. `NotificationService` parses the weather JSON, checks if `weather[].main` equals "severe"
5. If severe, a notification message is sent to the `weather.queue` JMS topic

### JSON Parsing Pattern

The codebase uses Jackson `ObjectMapper` with `TypeReference<Map<String, Object>>` to parse raw JSON strings from the OpenWeather API. Weather data is accessed via `body.get("weather")` as a `List<?>`, then iterating to check `weatherMap.get("main")`.

### Configuration (application.yml)

- PostgreSQL: `jdbc:postgresql://localhost:5432/weatherdb`
- Artemis: `tcp://localhost:61616`, user/password `artemis`
- OpenWeatherMap base URL: `https://api.openweathermap.org/data/2.5/weather`
- Server port: 9090

## Testing Infrastructure

`AbstractIntegrationTest` is the base class for all integration tests. It:
- Starts Testcontainers for PostgreSQL 16, ActiveMQ Artemis, and WireMock server
- Injects dynamic properties via `@DynamicPropertySource`
- Provides `getWireMockServer()` for test stub configuration

Tests use `Awaitility` for async assertions (5-60 second timeouts).

## File Reference

```
weather_notification/
  pom.xml                                          (parent POM)
  weather_notification-app/
    pom.xml                                        (module dependencies)
    src/main/java/com/example/weather/
      WeatherNotificationApplication.java           (@SpringBootApplication)
      WeatherApiController.java                    (REST endpoint)
      WeatherService.java                          (business logic + persistence)
      NotificationService.java                     (severe weather detection + JMS)
      JmsConfig.java                               (Artemis JMS config)
      WeatherApiConfig.java                        (OpenWeatherApiClient bean)
      client/OpenWeatherApiClient.java             (HTTP client)
      entity/WeatherRequest.java                   (JPA entity)
      repository/WeatherRequestRepository.java     (Spring Data JPA)
    src/main/resources/application.yml             (config)
    src/test/java/com/example/weather/
      AbstractIntegrationTest.java                 (test base: Testcontainers + WireMock)
      NotificationFlowIT.java                      (severe weather notification flow)
      WeatherApiFlowIT.java                        (API persistence + error handling)
```
