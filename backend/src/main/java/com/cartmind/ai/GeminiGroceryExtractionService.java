package com.cartmind.ai;

import com.cartmind.dto.StructuredGroceryListDto;
import com.cartmind.exception.AiExtractionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Service
public class GeminiGroceryExtractionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiGroceryExtractionService.class);

    private static final String SYSTEM_PROMPT = """
            You convert informal grocery notes into a structured shopping list for Indian quick-commerce (Instamart).
            - name: the item as the user wrote it.
            - searchQuery: a short product search phrase (type/brand), without quantities.
            - quantity: size text as written, e.g. "2 kg" or "1 pack"; "1" if none given.
            - count: how many units to buy, integer, minimum 1.
            - estimatedPrice: estimated INR price for the line. Omit it if you are unsure.
            - totalEstimatedBudget: the budget the user stated if any, otherwise the sum of estimates. Omit if unknown.
            Do not invent items that are not in the note.
            """;

    private static final String SCHEMA_JSON = """
            {"type":"object",
             "properties":{
               "totalEstimatedBudget":{"type":"number"},
               "items":{"type":"array","items":{"type":"object",
                 "properties":{
                   "name":{"type":"string"},
                   "searchQuery":{"type":"string"},
                   "quantity":{"type":"string"},
                   "count":{"type":"integer"},
                   "estimatedPrice":{"type":"number"}},
                 "required":["name","searchQuery","quantity","count"]}}},
             "required":["items"]}
            """;

    private final RestClient restClient;
    private final String model;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public GeminiGroceryExtractionService(@Value("${cartmind.ai.gemini.api-key}") String apiKey,
            @Value("${cartmind.ai.gemini.model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    @SuppressWarnings("unchecked")
    public StructuredGroceryListDto extractGroceryList(String note) {
        try {
            Map<String, Object> schema = mapper.readValue(SCHEMA_JSON, Map.class);

            Map<String, Object> body = Map.of(
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", note)))),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "responseJsonSchema", schema));

            String response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(response);
            String text = root.path("candidates").path(0).path("content")
                    .path("parts").path(0).path("text").asText("");

            if (text.isBlank()) {
                log.error("Gemini returned no content. promptFeedback={}, raw={}",
                        root.path("promptFeedback"), response);
                throw new AiExtractionException("Gemini returned an empty or blocked response.", null);
            }
            log.debug("Gemini structured output: {}", text);
            return mapper.readValue(text, StructuredGroceryListDto.class);

        } catch (AiExtractionException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("Gemini HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            String msg = switch (e.getStatusCode().value()) {
                case 400 -> "Gemini rejected the request. Check the API key and model name.";
                case 403 -> "Gemini denied access for this API key.";
                case 404 -> "Gemini model not found. Check GEMINI_MODEL.";
                case 429 -> "Gemini rate limit reached. Wait a minute and try again.";
                default -> "Gemini request failed (HTTP " + e.getStatusCode().value() + ").";
            };
            throw new AiExtractionException(msg, e);
        } catch (RuntimeException e) {
            log.error("Grocery extraction failed", e);
            throw new AiExtractionException("Could not process the AI response.", e);
        }
    }
}