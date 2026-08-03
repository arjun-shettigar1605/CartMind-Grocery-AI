package com.cartmind.dto;

public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String themePreference;
    private String token;

    public UserResponse(Long id, String email, String fullName, String themePreference, String token) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.themePreference = themePreference;
        this.token = token;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public String getToken() {
        return token;
    }
}