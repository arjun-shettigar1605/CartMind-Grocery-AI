package com.cartmind.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public class PreferencesRequest {
    @NotBlank(message = "Theme is required")
    @Pattern(regexp = "light|dark", message = "Theme must be 'light' or 'dark'")
    private String theme;

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}