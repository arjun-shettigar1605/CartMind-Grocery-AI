package com.cartmind.ai;

import com.cartmind.dto.CartPreviewDto;
import com.cartmind.dto.GroceryItemDto;
import com.cartmind.dto.MatchedItemDto;
import com.cartmind.exception.AiExtractionException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
public class GeminiProductResolverService {

    private static final Logger log = LoggerFactory.getLogger(GeminiProductResolverService.class);
    private static final int MAX_TURNS = 4;
    private static final int MAX_PRODUCTS_PER_SEARCH = 5;

    private static final String SYSTEM_PROMPT = """
            You are a grocery shopping assistant for Swiggy Instamart.
            For every requested item, call search_products with a short query. Issue all searches together in one step.
            Then choose the single best product and pack-size variation for each item:
            - Only choose in-stock variations that appear in the search results. Never invent IDs.
            - Match the requested size. For "2 litres" prefer a 1 litre pack with quantity 2, or a 2 litre pack
              with quantity 1, over a 500 ml pack.
            - Prefer the brand if the user names one. Otherwise prefer well-known brands and good value,
              and avoid promoted products when an equivalent exists.
            - quantity is the number of packs to buy so the total matches the requested amount.
            Finally call submit_selection once with all choices. If nothing suitable exists for an item,
            list its requested name under unavailable.
            """;

    private static final String TOOLS_JSON = """
            [{"functionDeclarations":[
              {"name":"search_products",
               "description":"Search Swiggy Instamart for products matching a short query.",
               "parameters":{"type":"object",
                 "properties":{"query":{"type":"string","description":"Short product search phrase"}},
                 "required":["query"]}},
              {"name":"submit_selection",
               "description":"Submit the final product choice for every requested item.",
               "parameters":{"type":"object",
                 "properties":{
                   "selections":{"type":"array","items":{"type":"object",
                     "properties":{
                       "requestedName":{"type":"string"},
                       "spinId":{"type":"string"},
                       "skuId":{"type":"string"},
                       "quantity":{"type":"integer","description":"Number of packs to buy"}},
                     "required":["requestedName","spinId","skuId","quantity"]}},
                   "unavailable":{"type":"array","items":{"type":"string"}}},
                 "required":["selections","unavailable"]}}
            ]}]
            """;

    public record Candidate(String productName, String variationName, String spinId,
            String skuId, double unitPrice, int maxQuantity) {
    }

    private final GeminiClient gemini;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public GeminiProductResolverService(GeminiClient gemini) {
        this.gemini = gemini;
    }

    @SuppressWarnings("unchecked")
    public CartPreviewDto resolve(List<GroceryItemDto> items, Function<String, JsonNode> searchTool) {
        Map<String, Candidate> candidates = new HashMap<>();
        List<Object> contents = new ArrayList<>();
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", describeItems(items)))));

        for (int turn = 1; turn <= MAX_TURNS; turn++) {
            log.info("Calling Gemini function calling, turn {}", turn);
            JsonNode response = gemini.generateContent(buildBody(contents));
            JsonNode content = response.path("candidates").path(0).path("content");

            List<JsonNode> calls = new ArrayList<>();
            for (JsonNode part : content.path("parts")) {
                if (part.has("functionCall"))
                    calls.add(part.get("functionCall"));
            }
            if (calls.isEmpty()) {
                throw new AiExtractionException("Gemini did not return a product selection.", null);
            }
            log.info("Gemini requested {} tool call(s)", calls.size());

            for (JsonNode call : calls) {
                if ("submit_selection".equals(call.path("name").asText())) {
                    return toPreview(call.path("args"), candidates, items);
                }
            }

            // Echo the model's content back exactly as received: this keeps the thought
            // signatures.
            contents.add(mapper.convertValue(content, Map.class));

            List<Object> responseParts = new ArrayList<>();
            for (JsonNode call : calls) {
                String query = call.path("args").path("query").asText();
                log.info("Tool call search_products: '{}'", query);
                JsonNode data = searchTool.apply(query);

                Map<String, Object> functionResponse = new HashMap<>();
                functionResponse.put("name", call.path("name").asText());
                functionResponse.put("response", Map.of("result", summarise(data, candidates)));
                if (call.has("id"))
                    functionResponse.put("id", call.get("id").asText());
                responseParts.add(Map.of("functionResponse", functionResponse));
            }
            contents.add(Map.of("role", "user", "parts", responseParts));
        }
        throw new AiExtractionException("Product selection did not finish in time.", null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildBody(List<Object> contents) {
        return Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", contents,
                "tools", mapper.readValue(TOOLS_JSON, List.class),
                "toolConfig", Map.of("functionCallingConfig", Map.of("mode", "ANY")));
    }

    private String describeItems(List<GroceryItemDto> items) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GroceryItemDto i : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", i.getName());
            m.put("quantity", i.getQuantity());
            m.put("count", i.getCount());
            list.add(m);
        }
        return "Grocery list: " + mapper.writeValueAsString(list);
    }

    /**
     * Trims Swiggy's search result and records every real candidate for later
     * validation.
     */
    private List<Map<String, Object>> summarise(JsonNode data, Map<String, Candidate> registry) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode p : data.path("products")) {
            if (out.size() >= MAX_PRODUCTS_PER_SEARCH)
                break;
            if (!p.path("inStock").asBoolean(false) || !p.path("isAvail").asBoolean(false))
                continue;

            List<Map<String, Object>> variations = new ArrayList<>();
            for (JsonNode v : p.path("variations")) {
                if (!v.path("isInStockAndAvailable").asBoolean(false))
                    continue;
                double price = v.path("price").path("offerPrice").asDouble(0);
                int maxQty = v.path("maxQuantity").asInt(Integer.MAX_VALUE);
                Candidate c = new Candidate(p.path("displayName").asText(), v.path("quantityDescription").asText(),
                        v.path("spinId").asText(), v.path("skuId").asText(), price, maxQty);
                registry.put(c.spinId() + "|" + c.skuId(), c);
                variations.add(Map.of("spinId", c.spinId(), "skuId", c.skuId(),
                        "size", c.variationName(), "price", price));
            }
            if (variations.isEmpty())
                continue;
            out.add(Map.of("product", p.path("displayName").asText(),
                    "promoted", p.path("isPromoted").asBoolean(false),
                    "variations", variations));
        }
        return out;
    }

    private CartPreviewDto toPreview(JsonNode args, Map<String, Candidate> candidates, List<GroceryItemDto> items) {
        List<MatchedItemDto> matched = new ArrayList<>();
        Set<String> matchedNames = new HashSet<>();

        for (JsonNode s : args.path("selections")) {
            String requested = s.path("requestedName").asText();
            Candidate c = candidates.get(s.path("spinId").asText() + "|" + s.path("skuId").asText());
            if (c == null) {
                log.warn("Model chose a product that was not in the search results for '{}'. Ignored.", requested);
                continue;
            }
            int qty = Math.max(1, Math.min(s.path("quantity").asInt(1), c.maxQuantity()));
            matched.add(new MatchedItemDto(requested, qty, c.productName(), c.variationName(),
                    c.spinId(), c.skuId(), c.unitPrice(), c.unitPrice() * qty));
            matchedNames.add(requested.toLowerCase());
        }
        List<String> unmatched = items.stream().map(GroceryItemDto::getName)
                .filter(n -> !matchedNames.contains(n.toLowerCase())).toList();
        double total = matched.stream().mapToDouble(MatchedItemDto::lineTotal).sum();
        return new CartPreviewDto(matched, unmatched, total);
    }
}