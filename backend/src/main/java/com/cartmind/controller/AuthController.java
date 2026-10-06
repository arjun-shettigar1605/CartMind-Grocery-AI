// package com.cartmind.controller;

package com.cartmind.controller;

import com.cartmind.dto.AuthRequest;
import com.cartmind.dto.UserResponse;
import com.cartmind.entity.User;
import com.cartmind.service.UserService;
import com.cartmind.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*") // Open for initial local frontend integration
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody AuthRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        UserResponse response = userService.loginUser(request);

        // Establish the HTTP Session Context for OAuth2 Client linking
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-swiggy")
    public void testSwiggyFlow(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        // 1. Ensure a test user exists
        User user = userRepository.findByEmail("test@example.com").orElseGet(() -> {
            User newUser = new User("test@example.com", "password", "Test User");
            return userRepository.save(newUser);
        });

        // 2. Establish the session context natively
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                Collections.emptyList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        // 3. Trigger the OAuth2 redirect
        response.sendRedirect("/api/oauth2/authorization/swiggy-mcp");
    }
} 
