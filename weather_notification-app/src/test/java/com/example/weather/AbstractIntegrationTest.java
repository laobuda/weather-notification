package com.example.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public abstract class AbstractIntegrationTest {

    protected static PostgreSQLContainer<?> postgresContainer;
    protected static WireMockServer wireMockServer;

    static {
        // Containers must be started in a static block so they are available when
        // @SpringBootTest initializes the Spring context (which happens before @BeforeAll).
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        postgresContainer.start();

        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }

    @BeforeAll
    static void startContainers() {
        // Intentionally empty — container startup moved to static initializer block.
        // Kept for subclasses that need to start additional resources.
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://"
                + postgresContainer.getHost()
                + ":" + postgresContainer.getMappedPort(5432)
                + "/" + postgresContainer.getDatabaseName());
        registry.add("spring.datasource.username", () -> postgresContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresContainer.getPassword());
        registry.add("weather.api.base-url", () -> "http://localhost:" + wireMockServer.port() + "/data/2.5/weather");
        registry.add("weather.api.forecast-url", () -> "http://localhost:" + wireMockServer.port() + "/weather/forecast");
    }

    @AfterEach
    void resetWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.resetAll();
        }
    }

    protected static WireMockServer getWireMockServer() {
        return wireMockServer;
    }
}
