package com.example.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.artemis.autoconfigure.ArtemisAutoConfiguration;

@SpringBootApplication(exclude = ArtemisAutoConfiguration.class)
public class WeatherNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherNotificationApplication.class, args);
    }
}
