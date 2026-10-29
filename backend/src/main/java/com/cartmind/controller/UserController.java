package com.cartmind.controller;

import com.cartmind.dto.PreferencesRequest;
import com.cartmind.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/preferences")
    public ResponseEntity<Map<String, String>> updatePreferences(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PreferencesRequest request) {
        userService.updateThemePreference(userId, request.getTheme());
        return ResponseEntity.ok(Map.of("theme", request.getTheme()));
    }
}