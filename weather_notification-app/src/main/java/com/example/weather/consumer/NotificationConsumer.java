package com.example.weather.consumer;

import com.example.weather.entity.WeatherRequest;
import com.example.weather.repository.WeatherRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * JMS consumer for weather notification messages.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WeatherRequestRepository repository;

    public NotificationConsumer(WeatherRequestRepository repository) {
        this.repository = repository;
    }

    /**
     * Listens for messages on the weather.queue and updates request status.
     *
     * @param message the JMS message
     */
    @JmsListener(destination = "weather.queue")
    public void receiveMessage(Message message) throws jakarta.jms.JMSException {
        String textMessage;
        if (message instanceof TextMessage textMsg) {
            textMessage = textMsg.getText();
        } else {
            textMessage = message.toString();
        }

        log.info("Received message from weather.queue: {}", textMessage);

        // Parse the JSON message using Jackson JsonNode instead of fragile string parsing
        String cityName = "unknown";
        String dateStr;
        try {
            JsonNode json = MAPPER.readTree(textMessage);
            cityName = json.path("city").asText("unknown");
            dateStr = json.path("date").asText("");
        } catch (Exception e) {
            log.error("Failed to parse notification message as JSON: {}", textMessage, e);
            return;
        }

        try {
            LocalDate parsedDate = (dateStr == null || dateStr.isEmpty())
                    ? LocalDate.now()
                    : LocalDate.parse(dateStr);
            List<WeatherRequest> existingRequests = repository
                    .findByCityNameAndRequestedDateOrderByCreatedAtDesc(cityName, parsedDate);

            if (!existingRequests.isEmpty()) {
                WeatherRequest existing = existingRequests.get(0);
                existing.setStatus(WeatherRequest.Status.SUCCESS);
                repository.save(existing);
                log.info("Successfully updated notification for city: {}", cityName);
            } else {
                log.warn("No existing WeatherRequest found for city: {} date: {}", cityName, dateStr);
            }
        } catch (Exception e) {
            log.error("Error processing message from weather.queue for city: {}", cityName, e);
        }
    }
}
