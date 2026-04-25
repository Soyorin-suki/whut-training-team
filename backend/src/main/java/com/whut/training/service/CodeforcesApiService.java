package com.whut.training.service;

import com.whut.training.aspect.annotation.ServiceLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.domain.entity.CfProblem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ServiceLog
public class CodeforcesApiService {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodeforcesApiService(@Value("${codeforces.base_url:https://codeforces.com/api}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Optional<CodeforcesUserProfile> getUserInfo(String handle) {
        if (handle == null || handle.isBlank()) {
            return Optional.empty();
        }

        String encodedHandle = URLEncoder.encode(handle.trim(), StandardCharsets.UTF_8);
        String url = baseUrl + "/user.info?handles=" + encodedHandle;
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

    public List<CfProblem> fetchProblemSet() {
        String url = baseUrl + "/problemset.problems";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!"OK".equalsIgnoreCase(root.path("status").asText())) {
                return List.of();
            }

            JsonNode result = root.path("result");
            JsonNode problemsNode = result.path("problems");
            JsonNode statsNode = result.path("problemStatistics");
            if (!problemsNode.isArray() || !statsNode.isArray()) {
                return List.of();
            }

            Map<String, Integer> solvedCountMap = new HashMap<>();
            for (JsonNode stat : statsNode) {
                Integer contestId = nullableInt(stat, "contestId");
                String index = nullableText(stat, "index");
                Integer solvedCount = nullableInt(stat, "solvedCount");
                if (contestId == null || index == null || solvedCount == null) {
                    continue;
                }
                solvedCountMap.put(buildProblemKey(contestId, index), solvedCount);
            }

            List<CfProblem> problems = new ArrayList<>();
            for (JsonNode node : problemsNode) {
                Integer contestId = nullableInt(node, "contestId");
                String problemIndex = nullableText(node, "index");
                String name = nullableText(node, "name");
                if (contestId == null || problemIndex == null || name == null) {
                    continue;
                }
                if (contestId >= 100000) {
                    continue;
                }
                String problemKey = buildProblemKey(contestId, problemIndex);
                Integer sourceContestId = nullableInt(node, "sourceContestId");
                Integer rating = nullableInt(node, "rating");
                boolean interactive = node.path("interactive").asBoolean(false);
                String tags = readTags(node.path("tags"));
                Integer solvedCount = solvedCountMap.get(problemKey);
                String sourceUrl = "https://codeforces.com/problemset/problem/" + contestId + "/" + problemIndex;
                problems.add(new CfProblem(
                        problemKey,
                        contestId,
                        problemIndex,
                        name,
                        rating,
                        tags,
                        interactive,
                        sourceContestId,
                        solvedCount,
                        sourceUrl
                ));
            }
            return problems;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public Optional<SubmissionStatus> getSubmissionStatus(String handle, Long submissionId) {
        if (handle == null || handle.isBlank() || submissionId == null) {
            return Optional.empty();
        }

        String encodedHandle = URLEncoder.encode(handle.trim(), StandardCharsets.UTF_8);
        String url = baseUrl + "/user.status?handle=" + encodedHandle + "&from=1&count=1000";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(8))
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
            if (!result.isArray()) {
                return Optional.empty();
            }

            for (JsonNode row : result) {
                Long id = nullableLong(row, "id");
                if (id == null || !id.equals(submissionId)) {
                    continue;
                }
                JsonNode problem = row.path("problem");
                Integer contestId = nullableInt(problem, "contestId");
                String index = nullableText(problem, "index");
                String verdict = nullableText(row, "verdict");
                Long creationTimeSeconds = nullableLong(row, "creationTimeSeconds");
                if (contestId == null || index == null) {
                    return Optional.empty();
                }
                return Optional.of(new SubmissionStatus(
                        submissionId,
                        contestId,
                        index,
                        verdict == null ? "UNKNOWN" : verdict,
                        creationTimeSeconds == null ? null : Instant.ofEpochSecond(creationTimeSeconds)
                ));
            }
            return Optional.empty();
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

    private String readTags(JsonNode tagsNode) {
        if (!tagsNode.isArray()) {
            return "";
        }
        List<String> tags = new ArrayList<>();
        for (JsonNode tagNode : tagsNode) {
            String tag = tagNode.asText();
            if (tag != null && !tag.isBlank()) {
                tags.add(tag);
            }
        }
        return String.join(",", tags);
    }

    private String buildProblemKey(Integer contestId, String problemIndex) {
        return contestId + "-" + problemIndex;
    }

    public record CodeforcesUserProfile(Integer rating, Integer maxRating, Boolean online,
                                        Long lastOnlineTimeSeconds, String avatarUrl) {
    }

    public record SubmissionStatus(Long submissionId, Integer contestId, String problemIndex, String verdict,
                                   Instant creationTime) {
    }
}
