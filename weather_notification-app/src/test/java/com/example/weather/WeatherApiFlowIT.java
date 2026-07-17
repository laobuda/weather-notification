package com.example.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.artemis.broker-url=tcp://localhost:61616",
                "spring.artemis.user=admin",
                "spring.artemis.password=admin",
                "spring.artemis.mode=native",
                "spring.artemis.listeners.auto-create-queue=true",
                "weather.api.base-url=http://localhost:8089/data/2.5/weather"
        }
)
public class WeatherApiFlowIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private static WireMockServer wireMockServer;

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://"
                + postgresContainer.getHost()
                + ":" + postgresContainer.getMappedPort(5432)
                + "/" + postgresContainer.getDatabaseName());
        registry.add("spring.datasource.username", () -> postgresContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresContainer.getPassword());
    }

    @org.junit.jupiter.api.BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void resetWireMock() {
        if (wireMockServer != null) {
            wireMockServer.resetAll();
        }
    }

    @Test
    void testWeatherApiPersistence() {
        // Configure WireMock to return mock weather data
        String mockResponse = """
                {
                    "name": "London",
                    "main": {
                        "temp": 283.15,
                        "humidity": 80
                    },
                    "weather": [{"description": "clear sky", "icon": "01d"}]
                }
                """;

        WireMock.stubFor(get(urlPathEqualTo("/data/2.5/weather"))
                .withQueryParam("q", WireMock.equalTo("London"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // Call the REST endpoint via HTTP
        String baseUrl = "http://localhost:" + port;
        String url = baseUrl + "/api/weather/London/2024-07-15";

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "Expected 200 from weather API endpoint");
        });

        // Verify the WeatherRequest was persisted with status=SUCCESS
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            var requests = weatherRequestRepository.findByCityName("London");
            assertNotNull(requests);
            assertTrue(requests.size() > 0, "Expected at least one WeatherRequest to be persisted");

            WeatherRequest request = requests.get(0);
            assertEquals("London", request.getCityName());
            assertEquals(WeatherRequest.Status.SUCCESS, request.getStatus());
            assertNotNull(request.getResponsePayload());
            assertNotNull(request.getRequestedDate());
        });
    }

    @Test
    void testWeatherApiErrorHandling() {
        // Configure WireMock to return 404 (city not found)
        WireMock.stubFor(get(urlPathEqualTo("/data/2.5/weather"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"City not found\"}")));

        String baseUrl = "http://localhost:" + port;
        String url = baseUrl + "/api/weather/NonExistentCity/2024-07-15";

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                // 404 from OpenWeather should result in a 500 from our API (since we treat it as an error)
                // or the endpoint should handle it gracefully
                assertTrue(response.statusCode() == 200 || response.statusCode() == 500,
                        "Expected 200 or 500, got " + response.statusCode());
            } catch (Exception e) {
                // Network error is also acceptable
            }

            var requests = weatherRequestRepository.findByCityName("NonExistentCity");
            assertNotNull(requests);
            assertTrue(requests.size() > 0, "Expected a WeatherRequest to be persisted even for errors");

            WeatherRequest request = requests.get(0);
            assertEquals("NonExistentCity", request.getCityName());
            assertTrue(request.getStatus() == WeatherRequest.Status.NOT_FOUND
                    || request.getStatus() == WeatherRequest.Status.ERROR);
        });
    }
}
