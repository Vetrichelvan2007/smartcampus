package com.vetri.smartcampus.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetri.smartcampus.models.student.ComicLearningRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LocalComicLearningClient implements ComicLearningClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI generateUri;

    public LocalComicLearningClient(
            @Value("${comic.learning.api-base-url:http://localhost:8000}") String apiBaseUrl
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        String normalizedBaseUrl = apiBaseUrl.endsWith("/")
                ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1)
                : apiBaseUrl;
        this.generateUri = URI.create(normalizedBaseUrl + "/api/generate");
    }

    @Override
    public void streamComic(ComicLearningRequest request, OutputStream outputStream) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("material_content", request.getMaterialContent());
        payload.put("user_query", request.getUserQuery());
        payload.put("student_level", request.getStudentLevel());
        payload.put("panel_count", request.getPanelCount());

        HttpRequest httpRequest = HttpRequest.newBuilder(generateUri)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            String errorBody;
            try (InputStream errorStream = response.body()) {
                errorBody = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IOException("Comic API returned " + response.statusCode() + ": " + truncate(errorBody));
        }

        try (InputStream inputStream = response.body()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                outputStream.flush();
            }
        }
    }

    private static String truncate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "empty response";
        }
        String normalized = raw.replaceAll("\\s+", " ").trim();
        return normalized.length() > 280 ? normalized.substring(0, 280) + "..." : normalized;
    }
}
