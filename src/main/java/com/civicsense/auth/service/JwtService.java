package com.civicsense.auth.service;

import com.civicsense.user.entity.User;

public interface JwtService {

    String generateToken(User user);
}