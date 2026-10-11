package com.cartmind.exception;

public class SwiggyConnectionException extends RuntimeException {
    private final String code;

    public SwiggyConnectionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
