# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Weather Notification Service — a Spring Boot application that fetches weather data from the OpenWeatherMap API, detects severe weather conditions, and sends notifications via a JMS queue. Built with Java 21 and Spring Boot 3.x.

## Key Commands

```bash
# Build
mvn compile

# Run unit tests
mvn test

# Run a single unit test
mvn test -Dtest=ClassName

# Run integration tests (uses Testcontainers for PostgreSQL + ActiveMQ)
mvn verify -Dtest=ClassName

# Full build with tests
mvn clean verify
```

## Architecture

### Entry Points
- `WeatherNotificationApplication` — Spring Boot main class

### Controllers
- `WeatherApiController` (`/api/weather/{cityName}/{date}`) — HTTP endpoint that triggers weather processing

### Services
- `WeatherService` — Core business logic: fetches weather/forecast data from OpenWeatherMap, persists requests to PostgreSQL, merges current weather with forecast data. Has `getWeather()` (HTTP-triggered) and `processNotification()` (JMS-triggered) entry points.
- `NotificationService` — Orchestrates the notification flow: calls `WeatherService`, checks for severe weather (`"severe"` in `weather[].main`), and sends JSON messages to the JMS queue.

### Messaging
- `JmsConfig` — Spring Artemis JMS configuration
- `NotificationService` — producer (sends to `weather.queue`)
- `NotificationConsumer` — consumer (listens on `weather.queue`), parses JSON messages and updates persisted `WeatherRequest` records

### External API
- `OpenWeatherApiClient` — Plain HTTP client using `HttpURLConnection` (no Spring RestTemplate) to call OpenWeatherMap. Returns raw JSON strings.
- `WeatherApiConfig` — Configuration class that creates the client with configured base URL and API key.

### Data Layer
- `WeatherRequest` entity with `Status` enum (PENDING, SUCCESS, ERROR, NOT_FOUND)
- `WeatherRequestRepository` — Spring Data JPA repository
- PostgreSQL database (local: `localhost:5432/weatherdb`)

### Configuration
- `application.yml` — All external configuration: datasource, JPA, Artemis broker, OpenWeatherMap API URLs and key.

## Notification Flow

1. HTTP request hits `WeatherApiController.getWeather(cityName, date)`
2. `WeatherService.getWeather()` fetches current weather + forecast, merges them, persists to DB
3. `NotificationService.processWeatherRequest()` receives the response, checks for severe weather
4. If severe, a JSON message (`{city, date, severity}`) is sent to `weather.queue` via JMS
5. `NotificationConsumer.receiveMessage()` consumes from the queue and updates the persisted `WeatherRequest` status

## Testing

Integration tests use Testcontainers (PostgreSQL + ActiveMQ) and WireMock for external API mocking:

- `AbstractIntegrationTest` — Base class with Testcontainer setup (PostgreSQL container, ActiveMQ container, WireMock)
- `NotificationFlowIT` — Full notification flow test (HTTP → weather fetch → JMS → consumer)
- `WeatherApiFlowIT` — Weather API integration test

Test dependencies include: Testcontainers (PostgreSQL, ActiveMQ), WireMock, Awaitility.

## Code Style Notes

- No external HTTP client libraries — `OpenWeatherApiClient` uses raw `HttpURLConnection`
- JSON parsing uses Jackson `JsonNode`/`ObjectMapper` rather than string manipulation
- The `mergeWeatherResponses()` method converts `JsonNode` to mutable `Map`/`List` structures for recursive merging, then converts back
- Notification messages are plain JSON strings (not JMS objects) for simplicity
