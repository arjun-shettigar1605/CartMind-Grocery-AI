package com.cartmind.exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SwiggyConnectionException.class)
    public ResponseEntity<Map<String, String>> connection(SwiggyConnectionException e) {
        log.error("Swiggy connection error", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }

    @ExceptionHandler(SwiggyMcpException.class)
    public ResponseEntity<Map<String, String>> mcp(SwiggyMcpException e) {
        log.error("Swiggy MCP error", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("code", "SWIGGY_MCP_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(AiExtractionException.class)
    public ResponseEntity<Map<String, String>> ai(AiExtractionException e) {
        log.error("AI extraction error", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("code", "AI_EXTRACTION_FAILED", "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Unexpected server error."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}