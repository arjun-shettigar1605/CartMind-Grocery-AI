package com.cartmind.ai;

import com.cartmind.exception.AiExtractionException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final String model;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public GeminiClient(@Value("${cartmind.ai.gemini.api-key}") String apiKey,
            @Value("${cartmind.ai.gemini.model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    public JsonNode generateContent(Map<String, Object> body) {
        try {
            String response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return mapper.readTree(response);
        } catch (RestClientResponseException e) {
            log.error("Gemini HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            String msg = switch (e.getStatusCode().value()) {
                case 400 -> "Gemini rejected the request.";
                case 403 -> "Gemini denied access for this API key.";
                case 404 -> "Gemini model not found. Check GEMINI_MODEL.";
                case 429 -> "Gemini rate limit reached. Wait a minute and try again.";
                default -> "Gemini request failed (HTTP " + e.getStatusCode().value() + ").";
            };
            throw new AiExtractionException(msg, e);
        } catch (RuntimeException e) {
            log.error("Gemini call failed", e);
            throw new AiExtractionException("Could not reach Gemini.", e);
        }
    }
}