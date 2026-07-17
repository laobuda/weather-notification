package com.example.weather;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final WeatherRequestRepository weatherRequestRepository;

    public NotificationConsumer(WeatherRequestRepository weatherRequestRepository) {
        this.weatherRequestRepository = weatherRequestRepository;
    }

    @JmsListener(destination = "weather.queue")
    public void receiveMessage(Message message) {
        try {
            String textMessage = null;
            if (message instanceof TextMessage textMsg) {
                textMessage = textMsg.getText();
            } else {
                textMessage = message.toString();
            }

            log.info("Received message from weather.queue: {}", textMessage);

            // Parse the city name from the message
            String cityName = parseCityName(textMessage);
            LocalDate requestedDate = LocalDate.now();

            // Store as a WeatherRequest entity with SUCCESS status
            WeatherRequest weatherRequest = new WeatherRequest();
            weatherRequest.setId(UUID.randomUUID().getMostSignificantBits());
            weatherRequest.setCityName(cityName);
            weatherRequest.setRequestedDate(requestedDate);
            weatherRequest.setRequestPayload(textMessage);
            weatherRequest.setResponsePayload("Consumed from weather.queue");
            weatherRequest.setStatus(WeatherRequest.Status.SUCCESS);
            weatherRequest.setCreatedAt(LocalDateTime.now());

            weatherRequestRepository.save(weatherRequest);

            log.info("Stored weather request for city: {} with status: {}", cityName, WeatherRequest.Status.SUCCESS);
        } catch (JMSException e) {
            log.error("Error processing JMS message", e);
        } catch (Exception e) {
            log.error("Error processing message from weather.queue", e);
        }
    }

    private String parseCityName(String message) {
        // Extract city name from message - try JSON format first
        if (message != null && message.contains("\"city\"")) {
            int cityStart = message.indexOf("\"city\"") + 8;
            int cityEnd = message.indexOf("\"", cityStart + 1);
            if (cityEnd > cityStart) {
                return message.substring(cityStart + 1, cityEnd);
            }
        }
        // Try to extract from "city:" prefix
        if (message != null && message.contains("city:")) {
            int start = message.indexOf("city:") + 5;
            int end = message.indexOf(" ", start);
            if (end == -1) {
                end = message.length();
            }
            return message.substring(start, end).trim();
        }
        // Default: use the whole message as city name
        return message != null ? message.trim() : "unknown";
    }
}
