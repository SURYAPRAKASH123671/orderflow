package com.orderflow.gateway.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public TokenResponse issueDemoToken(@RequestBody TokenRequest request) {
        String role = request.role() == null || request.role().isBlank() ? "CUSTOMER" : request.role();
        String token = jwtService.issueToken(request.userId(), request.email(), role);
        return new TokenResponse(token, "Bearer", 3600);
    }

    public record TokenRequest(@NotBlank String userId, @NotBlank @Email String email, String role) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
    }
}
