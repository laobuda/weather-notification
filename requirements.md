# Requirements: Weather Notification Service (Bootstrap Phase)

## 0. Project Stack Declaration
These are the technology choices for this project. Agent files derive all technology-specific behavior from this section.
* **Framework**: Spring Boot 4.1.0, Java 21
* **Messaging**: Apache Artemis (JMS) — queue name: `weather.queue`
* **Database**: PostgreSQL, JPA, Hibernate
* **HTTP Client**: Java `HttpURLConnection` (no httpclient5)
* **Testing**: Testcontainers (Active MQ Artemis, PostgreSQL, WireMock)

## 1. Project Bootstrap & Metadata
* [x] **Generate Multi-Module Project Structure**
    * Group ID: `com.example`
    * Version: `0.0.1-SNAPSHOT`
    * Java Version: `21`
    * Spring Boot Version: `4.1.0`
    * Packaging: `pom` (parent) with two child modules

## 1.5. Project Structure — Module Names
These are the folder/module names for this project. All references in this document and in `pom.xml` files derive module names from this section.
* **Base module name (application code)**: `weather_notification`
    * Directory: `weather_notification-app/`
    * Parent POM `artifactId`: `weather_notification` (mirrors the base module name)
    * Application module `artifactId`: `weather_notification-app`
    * Parent POM `name`: `weather_notification`

## 2. Module: weather_notification-app (Application Code)
Contains the application source code and its own integration tests.
* [x] **Core:** `org.springframework.boot:spring-boot-starter-web`
* [x] **Artemis Messaging:**
    * `org.springframework.boot:spring-boot-starter-artemis`
    * `org.apache.activemq:artemis-jakarta-client` (version compatible with Spring Boot 4.1.0)
* [x] **Database & Persistence:**
    * `org.springframework.boot:spring-boot-starter-data-jpa`
    * `org.postgresql:postgresql`
* [x] **Test Dependencies:**
    * `org.springframework.boot:spring-boot-starter-test` (Scope: test)
    * `org.testcontainers:junit-jupiter` (version compatible with Spring Boot 4.1.0)
    * `org.testcontainers:activemq` (version compatible with Spring Boot 4.1.0)
    * `org.testcontainers:postgresql` (version compatible with Spring Boot 4.1.0)

Components:
* [x] `WeatherNotificationApplication` — `@SpringBootApplication`
* [x] `NotificationConsumer` — `@JmsListener` on `weather.queue`
* [x] `WeatherApiController` — `@RestController` exposing `GET /api/weather/{cityName}/{yyyy-MM-dd}`
* [x] `WeatherService` — Orchestrates OpenWeather API call via `HttpURLConnection`, handles DTO mapping, and delegates persistence
* [x] `WeatherRequestRepository` — Spring Data JPA interface extending `JpaRepository<WeatherRequest, Long>`
* [x] `WeatherRequest` — JPA Entity storing `cityName`, `requestedDate`, `requestPayload`, `responsePayload`, `status` (`SUCCESS`/`NOT_FOUND`/`ERROR`), and `createdAt`
* [x] `OpenWeatherApiClient` — Thin wrapper using `HttpURLConnection` to fetch data from `api.openweathermap.org/data/2.5/weather` (or forecast/history endpoints)
* [x] `AbstractIntegrationTest` — base test class with Testcontainers Artemis & PostgreSQL
* [x] `NotificationFlowIT` — integration test extending `AbstractIntegrationTest`
* [x] `WeatherApiFlowIT` — integration test extending `AbstractIntegrationTest` verifying REST endpoint, OpenWeather mock (WireMock), and DB persistence

## 4. README Generation
* [x] **Generate `README.md`** — If `README.md` does not exist at the project root, create it using the following structure:
    * Project name: "Weather Notification Service"
    * Brief description: A Spring Boot service that consumes messages from an Apache Artemis queue (`weather.queue`), exposes a REST API to query historical/current weather by city and date, and persists successful requests/responses to PostgreSQL.
    * Technology stack (from Section 0): Spring Boot 4.1.0, Java 21, Apache Artemis, PostgreSQL, JPA/Hibernate, Testcontainers.
    * Module structure:
        * `weather_notification-app/` — Application code (WeatherNotificationApplication, NotificationConsumer, WeatherApiController, WeatherService, WeatherRequest entity, integration tests)
    * Build instructions:
        ```bash
        mvn clean verify   # builds and runs all integration tests with Testcontainers
        ```
    * Queue name: `weather.queue`
    * REST Endpoint: `GET /api/weather/{cityName}/{yyyy-MM-dd}`

## 5. Success Criterion
The modules must **build and test successfully**. Module names are derived from Section 1.5 (base name `weather_notification`).

| Module | Command | Result |
|---|---|---|
| `weather_notification-app` | `mvn clean verify` | **Pass** — `NotificationFlowIT.testNotificationFlow()` sends a message to Artemis queue and verifies the JMS listener received it. **Plus**: `WeatherApiFlowIT.testWeatherApiPersistence()` calls `/api/weather/{city}/{date}`, verifies WireMock returns data, and confirms a `WeatherRequest` entity is persisted to PostgreSQL with `status=SUCCESS` and populated `responsePayload`. |

## 6. Dependency Resolution Rule
Only **Spring Boot version** and **Java version** are specified explicitly. All other dependency versions are resolved via their respective BOMs:
* Spring Boot BOM (`spring-boot-starter-parent`) for Spring Boot starters
* Testcontainers BOM for Testcontainers modules
No hardcoded `<version>` tags on individual dependency coordinates.
* [~] **Exception:** `wiremock-standalone` requires explicit version `3.10.0` (not covered by any BOM). `jetty-server` was replaced by `wiremock-standalone` which bundles all Jetty dependencies.

