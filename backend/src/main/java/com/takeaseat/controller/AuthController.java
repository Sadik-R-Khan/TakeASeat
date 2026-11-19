package com.takeaseat.controller;

import com.takeaseat.dto.AuthDtos.AuthResponse;
import com.takeaseat.dto.AuthDtos.LoginRequest;
import com.takeaseat.dto.AuthDtos.RegisterRequest;
import com.takeaseat.security.UserPrincipal;
import com.takeaseat.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return new AuthResponse(
                null,
                principal.getId(),
                principal.getName(),
                principal.getEmail(),
                principal.getRole().name()
        );
    }
}
