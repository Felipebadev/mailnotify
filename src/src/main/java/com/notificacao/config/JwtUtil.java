package com.notificacao.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms:86400000}") // 24h
    private long expirationMs;

    private Key signingKey;

    @PostConstruct
    void init() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            // Em produção, prefira falhar. Para DEV, usamos um fallback grande.
            log.warn("jwt.secret ausente; usando fallback DEV (não use em produção).");
            secretKey = "this_is_a_dev_secret_key_with_at_least_32_chars_123456";
        }

        byte[] keyBytes = decodeOrUtf8(secretKey.trim());

        // Garante >= 32 bytes (256 bits). Em DEV, derivamos via SHA-256.
        if (keyBytes.length < 32) {
            log.warn("jwt.secret com {} bytes (<32). Derivando com SHA-256 (DEV).", keyBytes.length);
            keyBytes = sha256(secretKey.trim());
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT inicializado (chave efetiva: {} bytes).", keyBytes.length);
    }

    /** Tenta Base64; se falhar (DecodingException/IAE), usa texto puro UTF-8. */
    private byte[] decodeOrUtf8(String value) {
        try {
            return Decoders.BASE64.decode(value);
        } catch (DecodingException | IllegalArgumentException e) {
            // Não é Base64 padrão → trata como texto
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar SHA-256 do jwt.secret", e);
        }
    }

    private Key getSigningKey() {
        return this.signingKey;
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email != null && email.equalsIgnoreCase(userDetails.getUsername());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
