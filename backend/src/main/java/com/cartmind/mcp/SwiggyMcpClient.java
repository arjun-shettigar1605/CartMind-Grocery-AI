package com.cartmind.mcp;
import com.cartmind.exception.SwiggyMcpException;
import com.cartmind.exception.SwiggyConnectionException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class SwiggyMcpClient {

    private static final Logger log = LoggerFactory.getLogger(SwiggyMcpClient.class);

    private final RestClient restClient;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public SwiggyMcpClient(@Value("${cartmind.mcp.swiggy-instamart-url}") String url) {
        this.restClient = RestClient.builder().baseUrl(url).build();
    }

    /**
     * Performs the MCP handshake once; reuse the session for several tool calls.
     */
    public Session openSession(String accessToken) {
        ResponseEntity<String> init = post(accessToken, null, request(1, "initialize", Map.of(
                "protocolVersion", "2025-03-26",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "cartmind", "version", "0.1.0"))));
        unwrapRpc(init.getBody());
        String sessionId = init.getHeaders().getFirst("Mcp-Session-Id");
        post(accessToken, sessionId, Map.of("jsonrpc", "2.0", "method", "notifications/initialized"));
        return new Session(accessToken, sessionId);
    }

    public class Session {
        private final String token;
        private final String sessionId;
        private int nextId = 2;

        private Session(String token, String sessionId) {
            this.token = token;
            this.sessionId = sessionId;
        }

        /** Returns the tool's "data" payload. */
        public JsonNode callTool(String tool, Map<String, ?> arguments) {
            log.info("MCP tools/call: {}", tool);
            ResponseEntity<String> res = post(token, sessionId,
                    request(nextId++, "tools/call", Map.of("name", tool, "arguments", arguments)));
            JsonNode result = unwrapRpc(res.getBody());
            log.debug("MCP raw result for {}: {}", tool, result);

            JsonNode payload = result.has("structuredContent")
                    ? result.get("structuredContent")
                    : mapper.readTree(result.path("content").path(0).path("text").asText("{}"));

            if (result.path("isError").asBoolean(false) || !payload.path("success").asBoolean(true)) {
                throw new SwiggyMcpException(
                        payload.path("error").path("message").asText("Swiggy tool call failed: " + tool), null);
            }
            return payload.has("data") ? payload.get("data") : payload;
        }
    }

    private Map<String, Object> request(int id, String method, Map<String, ?> params) {
        return Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params);
    }

    private ResponseEntity<String> post(String token, String sessionId, Map<String, ?> body) {
        try {
            return restClient.post()
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.setContentType(MediaType.APPLICATION_JSON);
                        h.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
                        if (sessionId != null)
                            h.set("Mcp-Session-Id", sessionId);
                    })
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                throw new SwiggyConnectionException("SWIGGY_TOKEN_EXPIRED",
                        "Swiggy rejected your session. Please reconnect.");
            }
            throw new SwiggyMcpException("Swiggy MCP error " + e.getStatusCode() + ": "
                    + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new SwiggyMcpException("Could not reach Swiggy MCP: " + e.getMessage(), e);
        }
    }

    private JsonNode unwrapRpc(String body) {
        JsonNode root = mapper.readTree(extractJson(body));
        if (root.has("error")) {
            throw new SwiggyMcpException("MCP error: " + root.path("error").path("message").asText(), null);
        }
        return root.path("result");
    }

    /** Handles both plain JSON and SSE ("data: {...}") responses. */
    private String extractJson(String body) {
        if (body == null || body.isBlank())
            throw new SwiggyMcpException("Empty response from Swiggy MCP", null);
        String trimmed = body.trim();
        if (trimmed.startsWith("{"))
            return trimmed;
        String last = null;
        for (String line : trimmed.split("\\R")) {
            if (line.startsWith("data:"))
                last = line.substring(5).trim();
        }
        if (last == null)
            throw new SwiggyMcpException("Unrecognised MCP response: " + trimmed, null);
        return last;
    }
}