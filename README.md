# Weather Notification Service

A Spring Boot service that consumes messages from an Apache Artemis queue (`weather.queue`), exposes a REST API to query historical/current weather by city and date, and persists successful requests/responses to PostgreSQL.

## Technology Stack

- **Framework**: Spring Boot 4.1.0
- **Language**: Java 21
- **Messaging**: Apache Artemis (JMS)
- **Database**: PostgreSQL with JPA/Hibernate
- **HTTP Client**: Java `HttpURLConnection`
- **Testing**: Testcontainers (Artemis, PostgreSQL, WireMock)

## Module Structure

- `weather_notification-app/` -- Application code (WeatherNotificationApplication, NotificationConsumer, WeatherApiController, WeatherService, WeatherRequest entity, integration tests)

## Build Instructions

```bash
mvn clean verify   # builds and runs all integration tests with Testcontainers
```

## Queue Name

`weather.queue`

## REST Endpoint

`GET /api/weather/{cityName}/{yyyy-MM-dd}`

Returns weather data for the specified city and date. If the request has been previously made, cached results are returned. Otherwise, the service calls the OpenWeatherMap API and persists the result.

Additional endpoint:

`GET /api/weather/history/{cityName}`

Returns all historical weather requests for a given city.
