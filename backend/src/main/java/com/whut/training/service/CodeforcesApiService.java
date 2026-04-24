package com.whut.training.service;

import com.whut.training.aspect.annotation.ServiceLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Service
@ServiceLog
public class CodeforcesApiService {

    private String BASE_URL = "https://codeforces.com/api";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<CodeforcesUserProfile> getUserInfo(String handle) {
        if (handle == null || handle.isBlank()) {
            return Optional.empty();
        }

        String encodedHandle = URLEncoder.encode(handle.trim(), StandardCharsets.UTF_8);
        String url = BASE_URL + "/user.info?handles=" + encodedHandle;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!"OK".equalsIgnoreCase(root.path("status").asText())) {
                return Optional.empty();
            }

            JsonNode result = root.path("result");
            if (!result.isArray() || result.isEmpty()) {
                return Optional.empty();
            }

            JsonNode user = result.get(0);
            return Optional.of(new CodeforcesUserProfile(
                    nullableInt(user, "rating"),
                    nullableInt(user, "maxRating"),
                    nullableBoolean(user, "online"),
                    nullableLong(user, "lastOnlineTimeSeconds"),
                    nullableText(user, "titlePhoto")
            ));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Integer nullableInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private Long nullableLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private Boolean nullableBoolean(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    public record CodeforcesUserProfile(Integer rating, Integer maxRating, Boolean online,
                                        Long lastOnlineTimeSeconds, String avatarUrl) {
    }
}
