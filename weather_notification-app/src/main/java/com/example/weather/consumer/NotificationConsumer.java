package com.example.weather.consumer;

import com.example.weather.entity.WeatherRequest;
import com.example.weather.repository.WeatherRequestRepository;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final WeatherRequestRepository repository;

    public NotificationConsumer(WeatherRequestRepository repository) {
        this.repository = repository;
    }

    @JmsListener(destination = "weather.queue")
    public void receiveMessage(Message message) throws jakarta.jms.JMSException {
        String textMessage;
        if (message instanceof TextMessage textMsg) {
            textMessage = textMsg.getText();
        } else {
            textMessage = message.toString();
        }

        log.info("Received message from weather.queue: {}", textMessage);

        // Parse city and date from the JMS message, then update the existing WeatherRequest
        // (created synchronously by NotificationService) instead of creating a new one.
        String cityName = parseCityName(textMessage);
        String dateStr = parseDateFromMessage(textMessage);

        try {
            List<WeatherRequest> existingRequests = repository
                    .findByCityNameAndRequestedDateOrderByCreatedAtDesc(
                            cityName, dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now());

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

    private String parseCityName(String message) {
        if (message != null && message.contains("city:")) {
            int start = message.indexOf("city:") + 5;
            int end = message.indexOf(",", start);
            if (end == -1) {
                end = message.length();
            }
            return message.substring(start, end).trim();
        }
        return message != null ? message.trim() : "unknown";
    }

    private String parseDateFromMessage(String message) {
        if (message != null && message.contains("date:")) {
            int dateStart = message.indexOf("date:") + 5;
            int dateEnd = message.indexOf(",", dateStart);
            if (dateEnd == -1) {
                dateEnd = message.length();
            }
            String datePart = message.substring(dateStart, dateEnd).trim();
            // Extract just the date portion (YYYY-MM-DD) from "date: 2024-07-18, severity: ..."
            int commaIdx = datePart.indexOf(',');
            if (commaIdx > 0) {
                datePart = datePart.substring(0, commaIdx).trim();
            }
            return datePart;
        }
        return null;
    }
}
