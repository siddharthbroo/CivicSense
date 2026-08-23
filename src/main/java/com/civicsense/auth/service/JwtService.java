package com.civicsense.auth.service;

import com.civicsense.user.entity.User;

import java.util.UUID;

public interface JwtService {

    String generateToken(User user);

    UUID extractUserId(String token);

    boolean isTokenValid(String token);
}