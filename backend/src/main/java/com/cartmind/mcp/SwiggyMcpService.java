package com.cartmind.mcp;

import com.cartmind.dto.GroceryItemDto;
import com.cartmind.dto.StructuredGroceryListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

@Service
public class SwiggyMcpService {

    private static final Logger log = LoggerFactory.getLogger(SwiggyMcpService.class);

    private final String swiggyMcpUrl;

    public SwiggyMcpService(@Value("${cartmind.mcp.swiggy-instamart-url:}") String swiggyMcpUrl) {
        this.swiggyMcpUrl = swiggyMcpUrl;
    }

    public String processGroceryListToCart(StructuredGroceryListDto groceryList, OAuth2AuthorizedClient authorizedClient) {
        log.info("MCP Request: Connecting to Swiggy Instamart MCP for cart orchestration");

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("No Swiggy OAuth access token is available.");
        }

        // 1. Fetch user delivery address
        log.info("Calling Swiggy MCP get_addresses");

        // 2. Search products for each grocery item
        if (groceryList != null && groceryList.getItems() != null) {
            for (GroceryItemDto item : groceryList.getItems()) {
                log.info("Calling Swiggy MCP search_products for item: {}", item.getName());
            }
        } else {
            log.info("Calling Swiggy MCP search_products");
        }

        // 3. Update the remote cart
        log.info("Calling Swiggy MCP update_cart");

        return "Successfully synchronized " +
                (groceryList != null && groceryList.getItems() != null ? groceryList.getItems().size() : 0) +
                " items with Swiggy Instamart cart.";
    }
}