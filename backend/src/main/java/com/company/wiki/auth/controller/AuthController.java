package com.company.wiki.auth.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.company.wiki.auth.dto.TokenResponse;
import com.company.wiki.auth.service.AuthService;
import com.company.wiki.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
