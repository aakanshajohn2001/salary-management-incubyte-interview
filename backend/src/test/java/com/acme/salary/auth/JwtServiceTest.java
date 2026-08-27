package com.acme.salary.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-only-secret-value-at-least-32-bytes-long";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));

    @Test
    void issueThenParse_roundTripsUsernameAndRole() {
        JwtService.IssuedToken issued = jwtService.issueToken("hr.manager", Role.HR_MANAGER);

        var claims = jwtService.parse(issued.token()).orElseThrow();

        assertThat(claims.getSubject()).isEqualTo("hr.manager");
        assertThat(claims.get("role", String.class)).isEqualTo("HR_MANAGER");
    }

    @Test
    void parse_rejectsExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        String expiredToken = Jwts.builder()
                .subject("hr.manager")
                .claim("role", "HR_MANAGER")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThat(jwtService.parse(expiredToken)).isEmpty();
    }

    @Test
    void parse_rejectsTokenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService(new JwtProperties(
                "a-completely-different-unit-test-secret-value-32b", 60));
        JwtService.IssuedToken issued = otherService.issueToken("hr.manager", Role.HR_MANAGER);

        assertThat(jwtService.parse(issued.token())).isEmpty();
    }

    @Test
    void parse_rejectsGarbageInput() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
    }
}
