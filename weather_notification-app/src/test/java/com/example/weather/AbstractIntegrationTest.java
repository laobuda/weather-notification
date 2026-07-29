package com.example.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    protected static final ArtemisContainer artemis = new ArtemisContainer(
            DockerImageName.parse("apache/activemq-artemis:latest-alpine"))
            .withUser("artemis")
            .withPassword("artemis")
            .withReuse(false);

    protected static WireMockServer wireMockServer;

    static {
        postgresContainer.start();
        artemis.start();
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", () -> postgresContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresContainer.getPassword());
        registry.add("weather.api.base-url", () -> "http://localhost:" + wireMockServer.port() + "/data/2.5/weather");
        registry.add("weather.api.forecast-url", () -> "http://localhost:" + wireMockServer.port() + "/weather/forecast");
        registry.add("spring.artemis.broker-url", ()-> artemis.getBrokerUrl());
        registry.add("spring.artemis.authentication-enabled", () -> "false");
        registry.add("spring.artemis.use-jms-auth-enabled", () -> "false");
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
