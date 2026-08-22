package com.civicsense.auth.service;

import com.civicsense.auth.dto.LoginRequest;
import com.civicsense.auth.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}