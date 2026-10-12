package com.cartmind.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import com.cartmind.dto.AddressDto;
import com.cartmind.dto.CartLineDto;
import com.cartmind.dto.CartPreviewDto;
import com.cartmind.dto.CartPreviewRequest;
import com.cartmind.dto.CartSyncRequest;
import com.cartmind.dto.CartSyncResultDto;
import com.cartmind.dto.GroceryItemDto;
import com.cartmind.dto.MatchedItemDto;
import com.cartmind.service.SwiggyConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cartmind.ai.GeminiProductResolverService;
import com.cartmind.exception.AiExtractionException;
import java.util.function.Function;

@Service
public class SwiggyMcpService {

    private static final int MAX_ITEMS = 25;

    private final SwiggyMcpClient mcpClient;
    private final SwiggyConnectionService connectionService;
    private static final Logger log = LoggerFactory.getLogger(SwiggyMcpService.class);
    private final GeminiProductResolverService resolver;

    public SwiggyMcpService(SwiggyMcpClient mcpClient, SwiggyConnectionService connectionService, GeminiProductResolverService resolver) {
        this.mcpClient = mcpClient;
        this.connectionService = connectionService;
        this.resolver = resolver;
    }

    public List<AddressDto> getAddresses(String principalName) {
        var session = mcpClient.openSession(connectionService.requireAccessToken(principalName));
        JsonNode data = session.callTool("get_addresses", Map.of());
        List<AddressDto> addresses = new ArrayList<>();
        for (JsonNode a : data.path("addresses")) {
            addresses.add(new AddressDto(a.path("id").asText(), a.path("addressLine").asText(),
                    a.path("addressTag").asText("")));
        }
        return addresses;
    }

    public CartPreviewDto previewCart(String principalName, CartPreviewRequest req) {
        if (req.addressId() == null || req.addressId().isBlank())
            throw new IllegalArgumentException("Select a delivery address");
        if (req.items() == null || req.items().isEmpty())
            throw new IllegalArgumentException("Grocery list is empty");

        var session = mcpClient.openSession(connectionService.requireAccessToken(principalName));
        List<GroceryItemDto> items = req.items().stream().limit(MAX_ITEMS).toList();

        try {
            Function<String, JsonNode> searchTool = query -> session.callTool("search_products",
                    Map.of("addressId", req.addressId(), "query", query));
            return resolver.resolve(items, searchTool);
        } catch (AiExtractionException e) {
            log.warn("AI product resolution failed ({}). Falling back to heuristic matching.", e.getMessage());
            return previewWithHeuristics(session, req.addressId(), items);
        }
    }

    private CartPreviewDto previewWithHeuristics(SwiggyMcpClient.Session session,
            String addressId, List<GroceryItemDto> items) {
        List<MatchedItemDto> matched = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        for (GroceryItemDto item : items) {
            String query = (item.getSearchQuery() != null && !item.getSearchQuery().isBlank())
                    ? item.getSearchQuery()
                    : item.getName();
            int wanted = (item.getCount() != null && item.getCount() > 0) ? item.getCount() : 1;

            JsonNode data = session.callTool("search_products", Map.of("addressId", addressId, "query", query));
            pickBest(item.getName(), wanted, data.path("products"))
                    .ifPresentOrElse(matched::add, () -> unmatched.add(item.getName()));
        }
        double total = matched.stream().mapToDouble(MatchedItemDto::lineTotal).sum();
        return new CartPreviewDto(matched, unmatched, total);
    }

    /**
     * "Resolve products" v1: first in-stock, non-promoted product and its first
     * in-stock variation.
     */
    private Optional<MatchedItemDto> pickBest(String requestedName, int wanted, JsonNode products) {
        for (boolean allowPromoted : new boolean[] { false, true }) {
            for (JsonNode p : products) {
                if (!p.path("inStock").asBoolean(false) || !p.path("isAvail").asBoolean(false))
                    continue;
                if (p.path("isPromoted").asBoolean(false) && !allowPromoted)
                    continue;
                for (JsonNode v : p.path("variations")) {
                    if (!v.path("isInStockAndAvailable").asBoolean(false))
                        continue;
                    int qty = Math.min(wanted, v.path("maxQuantity").asInt(Integer.MAX_VALUE));
                    double price = v.path("price").path("offerPrice").asDouble(0);
                    return Optional.of(new MatchedItemDto(requestedName, qty,
                            p.path("displayName").asText(), v.path("quantityDescription").asText(),
                            v.path("spinId").asText(), v.path("skuId").asText(), price, price * qty));
                }
            }
        }
        return Optional.empty();
    }

    public CartSyncResultDto syncCart(String principalName, CartSyncRequest req) {
        if (req.addressId() == null || req.addressId().isBlank())
            throw new IllegalArgumentException("Select a delivery address");
        if (req.items() == null || req.items().isEmpty())
            throw new IllegalArgumentException("Nothing to add to the cart");

        var session = mcpClient.openSession(connectionService.requireAccessToken(principalName));
        JsonNode data = session.callTool("update_cart",
                Map.of("selectedAddressId", req.addressId(), "items", req.items()));

        List<String> warnings = new ArrayList<>();
        int removed = data.path("removedOutOfStockItems").size();
        if (removed > 0)
            warnings.add(removed + " item(s) went out of stock and were removed.");
        for (JsonNode r : data.path("reducedQuantityItems")) {
            warnings.add(r.path("itemName").asText() + ": quantity reduced from "
                    + r.path("requestedQuantity").asInt() + " to " + r.path("cappedQuantity").asInt());
        }
        return new CartSyncResultDto(req.items().size(), warnings);
    }
}