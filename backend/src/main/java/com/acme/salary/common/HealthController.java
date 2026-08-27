package com.acme.salary.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Unauthenticated liveness endpoint for the platform's deploy health check
 * (render.yaml's healthCheckPath) -- every other endpoint requires a JWT,
 * so the health checker needs something it can actually reach.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
