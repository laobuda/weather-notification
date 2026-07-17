package com.example.weather;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@SpringBootTest(
        properties = {
                "spring.artemis.broker-url=tcp://localhost:61616",
                "spring.artemis.user=admin",
                "spring.artemis.password=admin",
                "spring.artemis.mode=native",
                "spring.artemis.listeners.auto-create-queue=true",
                "weather.api.base-url=https://api.openweathermap.org/data/2.5/weather",
                "weather.api.api-key=dummy-key"
        }
)
class NotificationFlowIT extends AbstractIntegrationTest {

    private static final GenericContainer<?> artemisContainer = new GenericContainer<>(
            DockerImageName.parse("apache/activemq-artemis:latest-alpine"))
            .withExposedPorts(61616, 8161)
            .withEnv("ARTEMIS_USER", "admin")
            .withEnv("ARTEMIS_PASSWORD", "admin")
            .withLogConsumer(outputFrame -> {
                String log = outputFrame.getUtf8String();
                if (log.contains("AMQ211002") || log.contains("localhost:61616")) {
                    System.out.println("Artemis: " + log.trim());
                }
            })
            .withReuse(false);

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private NotificationService notificationService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://"
                + postgresContainer.getHost()
                + ":" + postgresContainer.getMappedPort(5432)
                + "/" + postgresContainer.getDatabaseName());
        registry.add("spring.datasource.username", () -> postgresContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresContainer.getPassword());
        registry.add("spring.artemis.broker-url", () -> "tcp://localhost:" + artemisContainer.getMappedPort(61616));
        registry.add("weather.api.base-url", () -> "http://localhost:" + getWireMockPort() + "/data/2.5/weather");
        registry.add("weather.api.api-key", () -> "test-api-key");
    }

    @BeforeAll
    static void startContainers() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        artemisContainer.start();
    }

    private static int getWireMockPort() {
        return wireMockServer.port();
    }

    @AfterAll
    static void stopContainers() {
        artemisContainer.stop();
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

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

        setField(weatherService, "forecastUrl",
                wireMockServer.baseUrl() + "/weather/forecast");
        setField(weatherService, "iconUrl",
                wireMockServer.baseUrl() + "/weather/icon");

        // When: Sending a request that triggers severe weather notification
        notificationService.processWeatherRequest("London", LocalDate.of(2024, 7, 15));

        // Wait for processing
        Thread.sleep(3000);

        // Then: The notification should be triggered
        java.util.List<WeatherRequest> savedRequests = weatherRequestRepository
                .findByCityNameAndRequestedDate("London", LocalDate.of(2024, 7, 15));
        WeatherRequest savedRequest = savedRequests.isEmpty()
                ? throwAssertionError("WeatherRequest not found")
                : savedRequests.get(0);

        assertThat(savedRequest.getStatus()).isEqualTo(WeatherRequest.Status.SUCCESS);
        assertThat(savedRequest.getResponsePayload()).contains("\"main\": \"severe\"");
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

        setField(weatherService, "forecastUrl",
                wireMockServer.baseUrl() + "/weather/forecast");
        setField(weatherService, "iconUrl",
                wireMockServer.baseUrl() + "/weather/icon");

        // When: Sending a request that does NOT trigger severe weather
        notificationService.processWeatherRequest("Paris", LocalDate.of(2024, 7, 15));

        // Wait for processing
        Thread.sleep(3000);

        // Then: The notification should NOT be triggered
        java.util.List<WeatherRequest> savedRequests = weatherRequestRepository
                .findByCityNameAndRequestedDate("Paris", LocalDate.of(2024, 7, 15));
        WeatherRequest savedRequest = savedRequests.isEmpty()
                ? throwAssertionError("WeatherRequest not found")
                : savedRequests.get(0);

        assertThat(savedRequest.getStatus()).isEqualTo(WeatherRequest.Status.SUCCESS);
        assertThat(savedRequest.getResponsePayload()).doesNotContain("\"severity\":\"severe\"");
    }

    private WeatherRequest throwAssertionError(String message) {
        throw new AssertionError(message);
    }
}
