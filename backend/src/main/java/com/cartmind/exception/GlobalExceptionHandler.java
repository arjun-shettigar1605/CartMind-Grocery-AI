package com.cartmind.exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SwiggyConnectionException.class)
    public ResponseEntity<Map<String, String>> connection(SwiggyConnectionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }

    @ExceptionHandler(SwiggyMcpException.class)
    public ResponseEntity<Map<String, String>> mcp(SwiggyMcpException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("code", "SWIGGY_MCP_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}