package com.retrobolsa.api.controller;

import com.retrobolsa.api.user.UserService;
import com.retrobolsa.api.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest request) throws Exception {
        userService.register(request);
    }
}
