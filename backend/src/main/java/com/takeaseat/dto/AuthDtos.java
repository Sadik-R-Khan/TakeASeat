package com.takeaseat.dto;

import com.takeaseat.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100)
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Valid email is required")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
            String password,

            Role role
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Valid email is required")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String name,
            String email,
            String role
    ) {}
}
