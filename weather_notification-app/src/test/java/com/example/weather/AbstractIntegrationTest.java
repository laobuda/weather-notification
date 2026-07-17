package com.example.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public abstract class AbstractIntegrationTest {

    protected static PostgreSQLContainer<?> postgresContainer;
    protected static WireMockServer wireMockServer;

    @Autowired
    protected WeatherRequestRepository weatherRequestRepository;

    @BeforeAll
    static void startContainers() {
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        postgresContainer.start();
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
