package com.retrobolsa.api.controller;

import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.security.LoginRateLimiter;
import com.retrobolsa.api.service.UserService;
import com.retrobolsa.api.user.dto.AuthResponse;
import com.retrobolsa.api.user.dto.LoginRequest;
import com.retrobolsa.api.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequest request) throws Exception {
        userService.register(request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        loginRateLimiter.checkAllowed(request.getEmail());
        String token = userService.login(request);
        AuthResponse response = new AuthResponse(token, "Bearer", jwtUtil.getExpirationMs());
        return ResponseEntity.ok(response);
    }
}
