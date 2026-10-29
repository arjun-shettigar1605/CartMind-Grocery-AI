package com.cartmind.service;

import com.cartmind.dto.AuthRequest;
import com.cartmind.dto.UserResponse;
import com.cartmind.entity.User;
import com.cartmind.repository.UserRepository;
import com.cartmind.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public UserResponse registerUser(AuthRequest request) {
        log.info("Auth Request: Registering new user with email {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Auth Exception: Registration failed. Email {} already exists.", request.getEmail());
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName()
        );

        User savedUser = userRepository.save(user);
        log.info("Database Event: Successfully stored user entity ID {}", savedUser.getId());

        String token = tokenProvider.generateToken(savedUser.getEmail());
        return new UserResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getThemePreference(), token);
    }

    public UserResponse loginUser(AuthRequest request) {
        log.info("Auth Request: Authenticating user email {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Auth Exception: Password mismatch for user {}", request.getEmail());
            throw new IllegalArgumentException("Invalid credentials");
        }

        log.info("Auth Success: User ID {} successfully authenticated", user.getId());
        String token = tokenProvider.generateToken(user.getEmail());
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getThemePreference(), token);
    }

    public void updateThemePreference(Long userId, String theme) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setThemePreference(theme);
        userRepository.save(user);
        log.info("Database Event: Updated theme preference for user ID {} to {}", userId, theme);
    }
}