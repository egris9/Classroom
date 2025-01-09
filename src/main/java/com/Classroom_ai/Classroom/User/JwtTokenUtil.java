package com.Classroom_ai.Classroom.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtTokenUtil {

    private static final String SECRET_KEY = "E0FOKBr7NIxNVKR3eWLZjDxtilkDk4zDzv11MuG7jKkdm10UMj5GtnvMRvLsY9wM/NRiaRg8cX57DuVZoP+h6Q=="; // Changez cela en une clé secrète plus sécurisée

    // Générer un token JWT
    public static String generateToken(User user) {
        // Convertir la clé secrète en objet Key
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

        // Créer et signer le JWT avec la nouvelle API
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // Expiration dans 24h
                .signWith(key, SignatureAlgorithm.HS256) // Utiliser la clé pour signer
                .compact();
    }

    // Valider le token JWT
    public static Claims validateToken(String token) {
        // Créer un JwtParser avec la nouvelle API
        JwtParser jwtParser = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes())) // Utilisez HMAC pour vérifier la signature
                .build();

        // Analyser le token et récupérer les informations (claims)
        return jwtParser.parseClaimsJws(token).getBody();
    }

    // Convertir la clé secrète en un objet Key
    private static Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());  // Générer la clé à partir du SECRET_KEY
    }


    // Extraire le sujet du token (ici, l'email de l'utilisateur)
    public static String getUserEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()  // Utilisation de la méthode moderne
                .setSigningKey(getSigningKey())  // Utilisation de la clé générée
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();  // Dans ce cas, le sujet du token est l'email de l'utilisateur
    }

    // Vérifier si le token est expiré
    public static boolean isTokenExpired(String token) {
        Claims claims = Jwts.parserBuilder()  // Utilisation de la méthode moderne
                .setSigningKey(getSigningKey())  // Utilisation de la clé générée
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().before(new Date());
    }
}