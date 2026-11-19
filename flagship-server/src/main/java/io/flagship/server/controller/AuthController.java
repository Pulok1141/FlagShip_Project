package io.flagship.server.controller;

import io.flagship.server.domain.User;
import io.flagship.server.repository.UserRepository;
import io.flagship.server.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public record LoginRequest(String email, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.email());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getGlobalRole());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "globalRole", user.getGlobalRole()
                )
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = auth.getName();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(userId);
        }

        return userOpt.map(user -> ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "globalRole", user.getGlobalRole()
        ))).orElseGet(() -> ResponseEntity.status(401).build());
    }
}
