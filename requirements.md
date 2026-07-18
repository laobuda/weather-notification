## [2026-07-18 10:00] ConflictingBeanDefinitionException due to duplicate OpenWeatherApiClient

**File(s):**
- `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/OpenWeatherApiClient.java`
- `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/client/OpenWeatherApiClient.java`

**Root Cause:**
There are two classes with the same name (`OpenWeatherApiClient`) in different packages: `com.example.weather` and `com.example.weather.client`.
Both are annotated with `@Component`, which causes a `ConflictingBeanDefinitionException` during Spring context loading because they both resolve to the default bean name `openWeatherApiClient`.

Based on `WeatherService.java` (line 2), the application expects the version in `com.example.weather.client` (which supports the `fetchWeather(String, String)` signature used in `WeatherService.java:31`). The version in `com.example.weather` appears to be a redundant, simpler version.

**Actionable Instructions:**
1. Delete the redundant class: `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/OpenWeatherApiClient.java`.
2. Verify that all imports in the codebase correctly point to `com.example.weather.client.OpenWeatherApiClient`.
