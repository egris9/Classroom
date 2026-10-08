package com.Classroom_ai.Classroom.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenUtil {

    private static final long TOKEN_LIFETIME_MS = 24 * 60 * 60 * 1000L;

    private final Key key;

    public JwtTokenUtil(@Value("${jwt.secret:}") String secret) {
        if (secret.isBlank()) {
            throw new IllegalStateException("jwt.secret is empty. Set the JWT_SECRET environment variable.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_LIFETIME_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Returns the email in the token. Throws {@link io.jsonwebtoken.JwtException} if the token is invalid or expired. */
    public String getUserEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
}
