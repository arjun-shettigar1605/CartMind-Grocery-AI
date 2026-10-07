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

    private void establishSession(User user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                Collections.emptyList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        UserResponse response = userService.registerUser(request);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        establishSession(user, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        UserResponse response = userService.loginUser(request);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        establishSession(user, httpRequest);
        return ResponseEntity.ok(response);
    }
} 
