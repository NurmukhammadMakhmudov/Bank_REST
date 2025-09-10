package com.example.bankcards.service;

import com.example.bankcards.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UsersService {
    private final UserRepository userRepository;

    public UsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }




}
