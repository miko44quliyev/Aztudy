package com.example.aztudyapigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    public void init() {
        log.info("🔑 JwtUtil initialized");
        log.info("🔑 Secret key loaded: {}", secret != null ? "YES (length: " + secret.length() + ")" : "NO - NULL!");

        if (secret == null || secret.length() < 32) {
            log.error("❌ JWT secret is null or too short! Length: {}", secret != null ? secret.length() : 0);
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
    }

    private SecretKey getSigningKey() {
        try {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("❌ Failed to create signing key: {}", e.getMessage());
            throw e;
        }
    }

    public Claims extractAllClaims(String token) {
        try {
            log.info("🔍 Attempting to parse token (length: {})", token.length());

            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.info("✅ Token parsed: subject={}, userId={}, roles={}",
                    claims.getSubject(),
                    claims.get("userId"),
                    claims.get("roles"));

            return claims;
        } catch (Exception e) {
            log.error("❌ Token parsing failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");

        if (userIdObj == null) {
            log.error("❌ userId claim is null");
            return null;
        }

        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }

        return Long.parseLong(userIdObj.toString());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = claims.get("roles", List.class);


        if (roles == null) {
            log.error("❌ roles claim is null");
        }
        return roles.stream()
                .filter(r -> r.startsWith("ROLE_"))
                .toList();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        boolean expired = expiration.before(new Date());

        if (expired) {
            log.warn("⚠️ Token expired at: {}", expiration);
        }

        return expired;
    }

    public boolean validateToken(String token) {
        try {
            boolean valid = !isTokenExpired(token);
            log.info("Token validation: {}", valid ? "✅ VALID" : "❌ EXPIRED");
            return valid;
        } catch (Exception e) {
            log.error("❌ Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}