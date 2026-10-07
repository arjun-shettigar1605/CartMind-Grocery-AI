package com.cartmind.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    public void oauth2Callback(Authentication authentication,
            HttpServletResponse response) throws IOException {
        if (authentication == null) {
            response.sendRedirect("http://localhost:5173/notes?swiggy=session_lost");
            return;
        }
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("swiggy-mcp", authentication.getName());
        if (client != null && client.getAccessToken() != null) {
            response.sendRedirect("http://localhost:5173/notes?swiggy=connected");
        } else {
            response.sendRedirect("http://localhost:5173/notes?swiggy=failed");
        }
    }
}