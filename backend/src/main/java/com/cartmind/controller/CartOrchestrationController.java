package com.cartmind.controller;

import com.cartmind.dto.StructuredGroceryListDto;
import com.cartmind.mcp.SwiggyMcpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartOrchestrationController {

    private final SwiggyMcpService swiggyMcpService;

    public CartOrchestrationController(SwiggyMcpService swiggyMcpService) {
        this.swiggyMcpService = swiggyMcpService;
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncToInstamart(
            @RequestBody StructuredGroceryListDto groceryList,
            @RegisteredOAuth2AuthorizedClient("swiggy-mcp") OAuth2AuthorizedClient authorizedClient) {

        // This endpoint requires a valid Swiggy OAuth token managed by Spring Security
        String result = swiggyMcpService.processGroceryListToCart(groceryList, authorizedClient);
        return ResponseEntity.ok(result);
    }
}