package com.cartmind.service;
import com.cartmind.exception.SwiggyConnectionException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class SwiggyConnectionService {

    public static final String REGISTRATION_ID = "swiggy-mcp";
    public enum Status { CONNECTED, NOT_CONNECTED, EXPIRED }

    private final OAuth2AuthorizedClientService clientService;

    public SwiggyConnectionService(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    public Status getStatus(String principalName) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(REGISTRATION_ID, principalName);
        if (client == null || client.getAccessToken() == null) return Status.NOT_CONNECTED;
        Instant expiresAt = client.getAccessToken().getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) return Status.EXPIRED;
        return Status.CONNECTED;
    }

    /** Used by the MCP service. Throws if the user must (re)connect. */
    public String requireAccessToken(String principalName) {
        Status status = getStatus(principalName);
        if (status == Status.NOT_CONNECTED) {
            throw new SwiggyConnectionException("SWIGGY_NOT_CONNECTED", "Connect your Swiggy account first.");
        }
        if (status == Status.EXPIRED) {
            throw new SwiggyConnectionException("SWIGGY_TOKEN_EXPIRED", "Your Swiggy session expired. Please reconnect.");
        }
        return clientService.loadAuthorizedClient(REGISTRATION_ID, principalName).getAccessToken().getTokenValue();
    }
}