package com.cartmind.ai;

import com.cartmind.dto.StructuredGroceryListDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cartmind.exception.AiExtractionException;

@Service
public class OpenAiGroceryExtractionService {

        private static final String SYSTEM_PROMPT = """
                        You convert informal grocery notes into a structured shopping list for Indian quick-commerce (Instamart).
                        - name: the item as the user wrote it.
                        - searchQuery: a short product search phrase (type/brand), without quantities.
                        - quantity: size text as written, e.g. "2 kg" or "1 pack"; "1" if none given.
                        - count: how many units to buy, integer, minimum 1.
                        - estimatedPrice: estimated INR price for the line, or null if unsure.
                        - totalEstimatedBudget: the budget the user stated if any, otherwise the sum of estimates, otherwise null.
                        Do not invent items that are not in the note.
                        """;

        private static final String SCHEMA_JSON = """
                        {"type":"object","additionalProperties":false,
                         "required":["items","totalEstimatedBudget"],
                         "properties":{
                           "totalEstimatedBudget":{"type":["number","null"]},
                           "items":{"type":"array","items":{"type":"object","additionalProperties":false,
                             "required":["name","searchQuery","quantity","count","estimatedPrice"],
                             "properties":{
                               "name":{"type":"string"},
                               "searchQuery":{"type":"string"},
                               "quantity":{"type":"string"},
                               "count":{"type":"integer"},
                               "estimatedPrice":{"type":["number","null"]}}}}}}
                        """;

        private final RestClient restClient;
        private final String model;
        private final JsonMapper mapper = JsonMapper.builder().build();
        private static final Logger log = LoggerFactory.getLogger(OpenAiGroceryExtractionService.class);

        public OpenAiGroceryExtractionService(@Value("${cartmind.ai.openai.api-key}") String apiKey,
                        @Value("${cartmind.ai.openai.model}") String model) {
                this.model = model;
                this.restClient = RestClient.builder()
                                .baseUrl("https://api.openai.com/v1")
                                .defaultHeader("Authorization", "Bearer " + apiKey)
                                .build();
        }

        public StructuredGroceryListDto extractGroceryList(String note) {
                try{        
                        Map<String, Object> body = Map.of(
                                        "model", model,
                                        "messages", List.of(
                                                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                                                        Map.of("role", "user", "content", note)),
                                        "response_format", Map.of("type", "json_schema",
                                                        "json_schema", Map.of("name", "grocery_list", "strict", true,
                                                                        "schema", mapper.readTree(SCHEMA_JSON))));

                        String response = restClient.post().uri("/chat/completions")
                                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);

                        String content = mapper.readTree(response).path("choices").path(0).path("message").path("content")
                                        .asText();
                        return mapper.readValue(content, StructuredGroceryListDto.class);
                } catch (RestClientResponseException e) {
                        log.error("OpenAI HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
                        String msg = switch (e.getStatusCode().value()) {
                                case 401 -> "OpenAI rejected the API key.";
                                case 429 -> "OpenAI quota or rate limit reached.";
                                default -> "OpenAI request failed (HTTP " + e.getStatusCode().value() + ").";
                        };
                        throw new AiExtractionException(msg, e);
                } catch (RuntimeException e) {
                        log.error("Grocery extraction failed", e);
                        throw new AiExtractionException("Could not process the AI response.", e);
                }

        }
}