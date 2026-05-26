package com.skillnet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import com.skillnet.persistence.entity.core.User;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final String secret;
    private final long expirationMs;

    public JwtService(
            @Value("${skillnet.jwt.secret}") String secret,
            @Value("${skillnet.jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String generateToken(CustomUserDetails userDetails) {
        return generateToken(userDetails, userDetails.getRole());
    }

    public String generateToken(CustomUserDetails userDetails, String activeRole) {
        String role = activeRole != null && !activeRole.isBlank() ? activeRole.trim() : userDetails.getRole();
        User user = userDetails.getUser();
        var builder = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs));
        if (user != null && !RoleAuthorityResolver.isAdminAccount(user)) {
            builder.claim("roles", List.of("student", "infoproductor"));
        } else if (user != null) {
            builder.claim("roles", List.of("admin"));
        }
        return builder.signWith(signingKey()).compact();
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> {
            Object role = claims.get("role");
            return role != null ? role.toString() : null;
        });
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
