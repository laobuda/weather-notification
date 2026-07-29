package com.example.weather;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

import com.example.weather.entity.WeatherRequest;
import com.example.weather.repository.WeatherRequestRepository;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class NotificationFlowIT extends AbstractIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WeatherRequestRepository weatherRequestRepository;

    @Test
    void testNotificationFlow_severeWeather() throws Exception {
        // Given: A weather forecast with severe weather
        ClassPathResource forecastResource = new ClassPathResource("forecast-severe.json");
        String forecastJson = new String(forecastResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        ClassPathResource iconResource = new ClassPathResource("icons/wind_40x40.png");
        try (InputStream iconStream = iconResource.getInputStream()) {
            ImageIO.read(iconStream);
        }

        // Configure WireMock for OpenWeather API
        String mockOpenWeatherResponse = """
                {
                    "name": "London",
                    "main": {"temp": 283.15, "humidity": 80},
                    "weather": [{"main": "severe", "description": "tornado", "icon": "13d"}]
                }
                """;
        wireMockServer.stubFor(get(urlPathMatching("/data/2.5/weather"))
                .willReturn(okJson(mockOpenWeatherResponse)));

        // Configure WireMock for weather forecast API
        wireMockServer.stubFor(get(urlPathMatching("/weather/forecast"))
                .willReturn(okJson(forecastJson)));

        // When: Sending a request that triggers severe weather notification
        notificationService.processWeatherRequest("London", LocalDate.of(2024, 7, 15));

        // Then: The notification should be triggered (await async processing)
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    java.util.List<WeatherRequest> savedRequests = weatherRequestRepository
                            .findByCityNameAndRequestedDateOrderByCreatedAtDesc("London", LocalDate.of(2024, 7, 15));
                    WeatherRequest savedRequest = savedRequests.isEmpty()
                            ? throwAssertionError("WeatherRequest not found")
                            : savedRequests.get(0);

                    assertThat(savedRequest.getStatus()).isEqualTo(WeatherRequest.Status.SUCCESS);
                    assertThat(savedRequest.getResponsePayload()).contains("\"main\":\"severe\"");
                });
    }

    @Test
    void testNotificationFlow_normalWeather() throws Exception {
        // Given: A normal weather forecast (no notification)
        ClassPathResource forecastResource = new ClassPathResource("forecast-normal.json");
        String forecastJson = new String(forecastResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        ClassPathResource iconResource = new ClassPathResource("icons/wind_40x40.png");
        try (InputStream iconStream = iconResource.getInputStream()) {
            ImageIO.read(iconStream);
        }

        // Configure WireMock for OpenWeather API
        String mockOpenWeatherResponse = """
                {
                    "name": "Paris",
                    "main": {"temp": 293.15, "humidity": 60},
                    "weather": [{"main": "clear", "description": "clear sky", "icon": "01d"}]
                }
                """;
        wireMockServer.stubFor(get(urlPathMatching("/data/2.5/weather"))
                .willReturn(okJson(mockOpenWeatherResponse)));

        // Configure WireMock for weather forecast API
        wireMockServer.stubFor(get(urlPathMatching("/weather/forecast"))
                .willReturn(okJson(forecastJson)));

        // When: Sending a request that does NOT trigger severe weather
        notificationService.processWeatherRequest("Paris", LocalDate.of(2024, 7, 15));

        // Then: The notification should NOT be triggered (await async processing)
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    java.util.List<WeatherRequest> savedRequests = weatherRequestRepository
                            .findByCityNameAndRequestedDateOrderByCreatedAtDesc("Paris", LocalDate.of(2024, 7, 15));
                    WeatherRequest savedRequest = savedRequests.isEmpty()
                            ? throwAssertionError("WeatherRequest not found")
                            : savedRequests.get(0);

                    assertThat(savedRequest.getStatus()).isEqualTo(WeatherRequest.Status.SUCCESS);
                    assertThat(savedRequest.getResponsePayload()).doesNotContain("\"main\":\"severe\"");
                });
    }

    private WeatherRequest throwAssertionError(String message) {
        throw new AssertionError(message);
    }
}
