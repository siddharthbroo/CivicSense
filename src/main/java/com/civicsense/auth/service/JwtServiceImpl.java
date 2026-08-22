package com.civicsense.auth.service;

import com.civicsense.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expiration = expiration;
    }

    @Override
    public String generateToken(User user) {

        Date issuedAt = new Date();
        Date expirationDate =
                new Date(issuedAt.getTime() + expiration);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("mobile", user.getMobileNumber())
                .claim(
                        "roles",
                        user.getRoles()
                                .stream()
                                .map(role -> role.getName())
                                .toList()
                )
                .issuedAt(issuedAt)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }
}