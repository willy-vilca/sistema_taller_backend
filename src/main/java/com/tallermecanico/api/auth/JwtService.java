package com.tallermecanico.api.auth;

import com.tallermecanico.api.user.SystemUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String createToken(SystemUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.getAccessTokenMinutes()));

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().getName().name())
                .claim("authVersion", user.getAuthVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Instant getExpirationInstant() {
        return Instant.now().plus(Duration.ofMinutes(properties.getAccessTokenMinutes()));
    }

    public Optional<String> extractUsername(String token) {
        return parseClaims(token).map(Claims::getSubject);
    }

    public boolean isTokenValid(String token, SystemUser user) {
        return parseClaims(token)
                .map(claims -> {
                    Object tokenVersion = claims.get("authVersion");
                    return user.getUsername().equalsIgnoreCase(claims.getSubject())
                            && tokenVersion instanceof Number version
                            && version.intValue() == user.getAuthVersion();
                })
                .orElse(false);
    }

    private Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
