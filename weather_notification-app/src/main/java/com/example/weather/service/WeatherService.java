package com.example.weather.service;

import com.example.weather.client.OpenWeatherApiClient;
import com.example.weather.entity.WeatherRequest;
import com.example.weather.repository.WeatherRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class WeatherService {

    private final OpenWeatherApiClient apiClient;
    private final WeatherRequestRepository repository;
    private final String forecastUrl;

    public WeatherService(OpenWeatherApiClient apiClient, WeatherRequestRepository repository,
                          String forecastUrl) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.forecastUrl = forecastUrl;
    }

    @Transactional
    public String getWeatherByCityAndDate(String cityName, LocalDate date) {
        String dateStr = date.toString();
        WeatherRequest request = new WeatherRequest();
        request.setCityName(cityName);
        request.setRequestedDate(date);
        request.setRequestPayload("{\"cityName\":\"" + cityName + "\", \"date\":\"" + dateStr + "\"}");
        request.setCreatedAt(LocalDateTime.now());

        try {
            String response = apiClient.fetchWeather(cityName, dateStr);
            if (response != null) {
                request.setStatus(WeatherRequest.Status.SUCCESS);
                // Merge forecast data if forecastUrl is set
                if (forecastUrl != null) {
                    String forecastJson = apiClient.fetchForecast(forecastUrl);
                    String merged = mergeWeatherResponses(response, forecastJson);
                    request.setResponsePayload(merged);
                } else {
                    request.setResponsePayload(response);
                }
            } else {
                request.setResponsePayload(null);
                request.setStatus(WeatherRequest.Status.NOT_FOUND);
            }
        } catch (Exception e) {
            request.setResponsePayload(e.getMessage());
            request.setStatus(WeatherRequest.Status.ERROR);
        }

        repository.save(request);
        return request.getResponsePayload();
    }

    @Transactional
    public String getWeather(String cityName, String date) {
        return getWeatherByCityAndDate(cityName, LocalDate.parse(date));
    }

    @Transactional
    public void processNotification(String message) {
        // Parse city and date from the JMS message
        String cityName = parseCityName(message);
        String dateStr = parseDateFromMessage(message);
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

        getWeatherByCityAndDate(cityName, date);
    }

    private String parseCityName(String message) {
        if (message != null && message.contains("\"city\"")) {
            int cityStart = message.indexOf("\"city\"") + 8;
            int cityEnd = message.indexOf("\"", cityStart + 1);
            if (cityEnd > cityStart) {
                return message.substring(cityStart + 1, cityEnd);
            }
        }
        if (message != null && message.contains("city:")) {
            int start = message.indexOf("city:") + 5;
            int end = message.indexOf(" ", start);
            if (end == -1) {
                end = message.length();
            }
            return message.substring(start, end).trim();
        }
        return message != null ? message.trim() : "unknown";
    }

    private String parseDateFromMessage(String message) {
        if (message != null && message.contains("\"date\"")) {
            int dateStart = message.indexOf("\"date\"") + 8;
            int dateEnd = message.indexOf("\"", dateStart + 1);
            if (dateEnd > dateStart) {
                return message.substring(dateStart + 1, dateEnd);
            }
        }
        return null;
    }

    /**
     * Merges current weather with forecast data.
     * Forecast fields that don't exist in current weather are added.
     * Weather array entries are merged (forecast fields override current).
     */
    @Transactional
    public String mergeWeatherResponses(String currentJson, String forecastJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Deep copy using ObjectMapper round-trip
            JsonNode currentCopy = mapper.readTree(currentJson);
            JsonNode merged = mergeJsonNodes(mapper, currentCopy, forecastJson);
            return mapper.writeValueAsString(merged);
        } catch (Exception e) {
            // If merge fails, return original
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

    /**
     * Extracts "main" from the first weather entry of the current response when it's a string
     * (e.g., "severe") and adds it as a top-level key in currentMap. This is called BEFORE
     * merging so the extracted value survives the merge where forecast entries override it.
     */
    private void extractWeatherMainAsString(JsonNode current, Map<String, Object> currentMap) {
        if (current == null || !current.has("weather") || current.get("weather").isEmpty()) {
            return;
        }
        JsonNode weatherArray = current.get("weather");
        if (weatherArray.isEmpty()) {
            return;
        }
        JsonNode firstWeather = weatherArray.get(0);
        if (firstWeather != null && firstWeather.has("main")) {
            JsonNode mainNode = firstWeather.get("main");
            if (mainNode.isTextual()) {
                currentMap.put("main", mainNode.asText());
            }
        }
    }

    private JsonNode mergeJsonNodes(ObjectMapper mapper, JsonNode current, JsonNode forecast) {
        // Serialize current to a mutable Map-based tree
        Map<String, Object> currentMap = jsonNodeToMap(mapper, current);
        // Extract "main" from the first weather entry of the CURRENT when it's a string
        // (e.g., "severe") before merge overwrites it with forecast values.
        extractWeatherMainAsString(current, currentMap);
        Map<String, Object> forecastMap = jsonNodeToMap(mapper, forecast);
        mergeMaps(currentMap, forecastMap);
        return mapper.valueToTree(currentMap);
    }

    /**
     * Converts JsonNode to a mutable Map structure for manipulation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonNodeToMap(ObjectMapper mapper, JsonNode node) {
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
            return null; // Signal to handle arrays separately
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
                if (forecastValue instanceof String && currentValue instanceof Map<?, ?> currentMap) {
                    mergeMaps((Map<String, Object>) currentMap, (Map<String, Object>) (Object) Map.of(key, forecastValue));
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
