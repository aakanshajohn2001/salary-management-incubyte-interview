package com.acme.salary.auth;

import java.time.Instant;

public record LoginResponse(String token, String username, String role, Instant expiresAt) {
}
