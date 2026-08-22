package com.civicsense.user.service;

import com.civicsense.user.dto.CreateUserRequest;
import com.civicsense.user.dto.CreateUserResponse;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request);
}