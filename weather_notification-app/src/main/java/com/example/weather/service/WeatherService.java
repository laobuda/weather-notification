package com.example.weather.service;

import com.example.weather.client.OpenWeatherApiClient;
import com.example.weather.entity.WeatherRequest;
import com.example.weather.repository.WeatherRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Service for fetching and caching weather data.
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OpenWeatherApiClient apiClient;
    private final WeatherRequestRepository repository;
    private final String forecastUrl;

    public WeatherService(OpenWeatherApiClient apiClient, WeatherRequestRepository repository,
                          @org.springframework.beans.factory.annotation.Qualifier("forecastUrl") String forecastUrl) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.forecastUrl = forecastUrl;
    }

    /**
     * Fetches weather data for a city and date, persisting the request/response.
     *
     * @param cityName the city name
     * @param date the date
     * @return the weather response JSON
     */
    @Transactional
    public String getWeatherByCityAndDate(String cityName, LocalDate date) {
        String dateStr = date.toString();
        WeatherRequest request = new WeatherRequest();
        request.setCityName(cityName);
        request.setRequestedDate(date);
        request.setRequestPayload(MAPPER.createObjectNode()
                .put("cityName", cityName)
                .put("date", dateStr)
                .toString());
        request.setCreatedAt(LocalDateTime.now());

        try {
            String response = apiClient.fetchWeather(cityName, dateStr);
            if (response != null) {
                request.setStatus(WeatherRequest.Status.SUCCESS);
                // Merge forecast data if forecastUrl is set
                if (forecastUrl != null) {
                    String forecastJson = apiClient.fetchForecast(forecastUrl);
                    if (forecastJson != null) {
                        String merged = mergeWeatherResponses(response, forecastJson);
                        request.setResponsePayload(merged);
                    } else {
                        request.setResponsePayload(response);
                    }
                } else {
                    request.setResponsePayload(response);
                }
            } else {
                request.setResponsePayload(null);
                request.setStatus(WeatherRequest.Status.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Failed to fetch weather for city: {}, date: {}", cityName, date, e);
            request.setResponsePayload(MAPPER.createObjectNode()
                    .put("error", e.getMessage())
                    .toString());
            request.setStatus(WeatherRequest.Status.ERROR);
        }

        repository.save(request);
        return request.getResponsePayload();
    }

    /**
     * Public API for fetching weather by city and date string.
     *
     * @param cityName the city name
     * @param date the date in yyyy-MM-dd format
     * @return the weather response JSON or error message
     */
    @Transactional
    public String getWeather(String cityName, String date) {
        try {
            return getWeatherByCityAndDate(cityName, LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format for city: {}, date: {}", cityName, date, e);
            return MAPPER.createObjectNode()
                    .put("error", "Invalid date format. Expected: yyyy-MM-dd")
                    .toString();
        }
    }

    /**
     * Processes a JMS notification message by fetching weather data.
     *
     * @param message the JMS message containing city and date
     */
    @Transactional
    public void processNotification(String message) {
        // Parse city and date from the JMS message
        String cityName = parseCityName(message);
        String dateStr = parseDateFromMessage(message);
        LocalDate date;
        try {
            date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("Invalid date in JMS message for city: {}, date: {}. Defaulting to today.", cityName, dateStr, e);
            date = LocalDate.now();
        }

        getWeatherByCityAndDate(cityName, date);
    }

    private String parseCityName(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        try {
            JsonNode json = MAPPER.readTree(message);
            return json.path("city").asText("unknown");
        } catch (Exception e) {
            log.warn("Failed to parse city from message as JSON: {}. Returning 'unknown'.", message, e);
            return "unknown";
        }
    }

    private String parseDateFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        try {
            JsonNode json = MAPPER.readTree(message);
            String dateValue = json.path("date").asText(null);
            return (dateValue != null && !dateValue.isEmpty()) ? dateValue : null;
        } catch (Exception e) {
            log.warn("Failed to parse date from message as JSON: {}. Returning null.", message, e);
            return null;
        }
    }

    /**
     * Merges current weather with forecast data.
     * Forecast fields that don't exist in current weather are added.
     * Weather array entries are merged (forecast fields override current).
     *
     * @param currentJson current weather JSON
     * @param forecastJson forecast weather JSON
     * @return merged JSON string
     */
    public String mergeWeatherResponses(String currentJson, String forecastJson) {
        try {
            // Deep copy using ObjectMapper round-trip
            JsonNode currentCopy = MAPPER.readTree(currentJson);
            JsonNode merged = mergeJsonNodes(MAPPER, currentCopy, forecastJson);
            return MAPPER.writeValueAsString(merged);
        } catch (Exception e) {
            // If merge fails, return original
            log.debug("Failed to merge weather responses, returning original", e);
            return currentJson;
        }
    }

    /**
     * Recursively merges forecast into current JsonNode.
     * Forecast fields override current only if they don't exist in current.
     */
    private JsonNode mergeJsonNodes(ObjectMapper mapper, JsonNode current, String forecastJson) throws Exception {
        if (forecastJson == null || forecastJson.isEmpty()) {
            return current;
        }
        JsonNode forecast = mapper.readTree(forecastJson);
        return mergeJsonNodes(mapper, current, forecast);
    }

    private JsonNode mergeJsonNodes(ObjectMapper mapper, JsonNode current, JsonNode forecast) {
        Object currentObj = jsonNodeToMap(mapper, current);
        if (currentObj instanceof Map) {
            Map<String, Object> currentMap = (Map<String, Object>) currentObj;
            Object forecastObj = jsonNodeToMap(mapper, forecast);
            if (forecastObj instanceof Map) {
                mergeMaps(currentMap, (Map<String, Object>) forecastObj);
            }
            return mapper.valueToTree(currentMap);
        }
        // Root is an array or primitive — return original current node
        return current;
    }

    /**
     * Converts JsonNode to a mutable Map or List structure for manipulation.
     * Returns Map for object nodes, List for array nodes, or a Map with "value" key for primitives.
     */
    @SuppressWarnings("unchecked")
    private Object jsonNodeToMap(ObjectMapper mapper, JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> {
                map.put(entry.getKey(), jsonNodeValue(mapper, entry.getValue()));
            });
            return map;
        }
        if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            node.forEach(item -> list.add(jsonNodeValue(mapper, item)));
            return list;
        }
        return Map.of("value", node.asText());
    }

    @SuppressWarnings("unchecked")
    private Object jsonNodeValue(ObjectMapper mapper, JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            if (node.isInt() || node.isLong()) {
                return node.asLong();
            }
            return node.asDouble();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isObject()) {
            return jsonNodeToMap(mapper, node);
        }
        if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            node.forEach(item -> list.add(jsonNodeValue(mapper, item)));
            return list;
        }
        return node.asText();
    }

    /**
     * Merges forecast map into current map. Forecast fields fill gaps in current.
     */
    @SuppressWarnings("unchecked")
    private void mergeMaps(Map<String, Object> current, Map<String, Object> forecast) {
        for (Map.Entry<String, Object> entry : forecast.entrySet()) {
            String key = entry.getKey();
            Object forecastValue = entry.getValue();
            if (!current.containsKey(key)) {
                current.put(key, forecastValue);
            } else {
                Object currentValue = current.get(key);
                // Recursively merge nested maps
                // When forecast is a string (e.g., "severe") and current is a Map (e.g., {temp, humidity}),
                // merge the forecast string into the existing Map so both are preserved.
                if (forecastValue instanceof String && currentValue instanceof Map<?, ?>) {
                    // Put the string value directly into the current map under the given key.
                    // This preserves the forecast string (e.g., "severe") alongside existing map entries.
                    ((Map<String, Object>) currentValue).put(key, forecastValue);
                } else if (currentValue instanceof Map<?, ?> currentMap && forecastValue instanceof Map<?, ?> forecastMap) {
                    mergeMaps((Map<String, Object>) currentMap, (Map<String, Object>) forecastMap);
                }
                // Merge arrays by index, merging weather entries
                if (currentValue instanceof java.util.List<?> currentList && forecastValue instanceof java.util.List<?> forecastList) {
                    java.util.List<Object> mergedList = new java.util.ArrayList<>();
                    for (int i = 0; i < Math.max(currentList.size(), forecastList.size()); i++) {
                        if (i < currentList.size() && i < forecastList.size()) {
                            Object cv = currentList.get(i);
                            Object fv = forecastList.get(i);
                            if (cv instanceof Map<?, ?> cmap && fv instanceof Map<?, ?> fmap) {
                                Map<String, Object> mergedMap = new java.util.LinkedHashMap<>((Map<String, Object>) cv);
                                mergeMaps(mergedMap, (Map<String, Object>) fv);
                                mergedList.add(mergedMap);
                            } else {
                                mergedList.add(fv);
                            }
                        } else {
                            mergedList.add(forecastList.get(i));
                        }
                    }
                    current.put(key, mergedList);
                }
            }
        }
    }
}
