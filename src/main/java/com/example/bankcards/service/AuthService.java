package com.example.bankcards.service;

import com.example.bankcards.dto.AuthRequest;
import com.example.bankcards.dto.RegistrationRequest;
import com.example.bankcards.dto.RegistrationResponse;
import com.example.bankcards.entity.User;

public interface AuthService {

    String authenticate(AuthRequest request);

    String authenticate(User request);

    RegistrationResponse register(RegistrationRequest request);
}