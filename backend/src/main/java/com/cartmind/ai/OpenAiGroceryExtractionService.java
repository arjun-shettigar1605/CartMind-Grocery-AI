package com.cartmind.ai;

import com.cartmind.dto.StructuredGroceryListDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;


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

        public OpenAiGroceryExtractionService(@Value("${cartmind.ai.openai.api-key}") String apiKey,
                        @Value("${cartmind.ai.openai.model}") String model) {
                this.model = model;
                this.restClient = RestClient.builder()
                                .baseUrl("https://api.openai.com/v1")
                                .defaultHeader("Authorization", "Bearer " + apiKey)
                                .build();
        }

        public StructuredGroceryListDto extractGroceryList(String note) {
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
        }
}