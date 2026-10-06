package com.cartmind.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    private final OAuth2AuthorizedClientService clientService;

    public RootController(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/")
    public ResponseEntity<String> rootSuccess() {
        return ResponseEntity.ok("Backend is running.");
    }

    @GetMapping("/login/oauth2/code/swiggy")
    public ResponseEntity<String> oauth2Callback(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Error: You lost your local JSESSIONID during the redirect.");
        }

        // Explicitly check if the filter successfully grabbed the Swiggy Token
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("swiggy-mcp", authentication.getName());

        if (client != null && client.getAccessToken() != null) {
            return ResponseEntity.ok("SUCCESS! The Swiggy token is now securely linked to your session. " +
                    "You can now test POST /api/cart/sync in Postman!");
        }

        return ResponseEntity.status(400)
                .body("Error: Token exchange failed. Ensure you registered a valid Client ID.");
    }
}