package com.takeaseat.service;

import com.takeaseat.dto.AuthDtos.AuthResponse;
import com.takeaseat.dto.AuthDtos.LoginRequest;
import com.takeaseat.dto.AuthDtos.RegisterRequest;
import com.takeaseat.entity.Role;
import com.takeaseat.entity.User;
import com.takeaseat.repository.UserRepository;
import com.takeaseat.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        // Public self-registration allows ROLE_USER or ROLE_OWNER (defaults to ROLE_USER).
        // Admin accounts are provisioned via seed data or database administration.
        Role role = request.role() == Role.ROLE_OWNER ? Role.ROLE_OWNER : Role.ROLE_USER;

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        return new AuthResponse(token, saved.getId(), saved.getName(), saved.getEmail(), saved.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
