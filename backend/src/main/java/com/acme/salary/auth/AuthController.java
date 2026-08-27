package com.acme.salary.auth;

import com.acme.salary.common.ApiError;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var user = appUserRepository.findByUsername(request.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            ApiError error = new ApiError(Instant.now(), 401, "Unauthorized",
                    "Invalid username or password", "/api/auth/login");
            return ResponseEntity.status(401).body(error);
        }

        JwtService.IssuedToken issued = jwtService.issueToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new LoginResponse(
                issued.token(), user.getUsername(), user.getRole().name(), issued.expiresAt()));
    }
}
