## [2026-07-18 10:05] ConflictingBeanDefinitionException due to duplicate OpenWeatherApiClient
- **File(s):**
    - `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/OpenWeatherApiClient.java`
    - `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/client/OpenWeatherApiClient.java`
- **Root Cause:** There are two classes with the same name (`OpenWeatherApiClient`) in different packages: `com.example.weather` and `com.example.weather.client`. Both are annotated with `@Component`, which causes a `ConflictingBeanDefinitionException` during Spring context loading because they both resolve to the default bean name `openWeatherApiClient`. Based on `WeatherService.java`, the application expects the version in `com.example.weather.client`.
- **Actionable Instructions:**
    1. Delete the redundant class: `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/OpenWeatherApiClient.java`.
    2. Verify that all imports in the codebase correctly point to `com.example.weather.client.OpenWeatherApiClient`.

## [2026-07-18 10:10] ConflictingBeanDefinitionException for WeatherApiController
- **File(s):**
    - `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/WeatherApiController.java`
    - `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/controller/WeatherApiController.java`
- **Root Cause:** Duplicate class definitions for `WeatherApiController` in the root package and the `.controller` package. Spring's component scanning identifies both as beans, resulting in a `ConflictingBeanDefinitionException` due to the same simple class name. The version in `.controller` follows standard MVC architecture.
- **Actionable Instructions:**
    1. Remove `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/WeatherApiController.java`.
    2. Retain `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/main/java/com/example/weather/controller/WeatherApiController.java` as it adheres to standard MVC patterns.
    3. Verify that all callers/tests are using the correct import: `com.example.weather.controller.WeatherApiController`.
