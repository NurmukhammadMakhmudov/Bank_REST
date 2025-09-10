package com.example.bankcards.service.Impl;

import com.example.bankcards.dto.AuthRequest;
import com.example.bankcards.dto.RegistrationRequest;
import com.example.bankcards.dto.RegistrationResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.CharBuffer;
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String authenticate(AuthRequest request) {
        log.debug("authenticate called: username={}", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        log.info("authenticate: user {} authenticated successfully", request.getUsername());
        return token;
    }

    @Override
    public String authenticate(User request) {
        log.debug("authenticate called: username={}", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        log.info("authenticate: user {} authenticated successfully", request.getUsername());
        return token;
    }

    @Override
    public RegistrationResponse register(RegistrationRequest request) {
        log.debug("register called: username={}", request.getUsername());
        if (userRepository.countUserByUsername(request.getUsername()) > 0) {
            log.warn("register: username {} already exists", request.getUsername());
            throw new RuntimeException("Пользователь с таким email уже существует");
        }
        User user = userRepository.save(toEntity(request));
        log.info("register: user {} registered successfully", user.getId());
        return toResponse(user);
    }

    private User toEntity(RegistrationRequest request) {
        String password = passwordEncoder.encode(CharBuffer.wrap(request.getPassword()));
        return new User(request.getStatus(), request.getRole(),
                request.getPassportNumber(), request.getPassportSeries(),
                request.getAddress(), request.getLastName(),
                request.getFirstName(), password, request.getUsername());
    }

    private RegistrationResponse toResponse(User user) {
        return new RegistrationResponse(user.getId(), user.getFirstName(), user.getLastName());
    }
}
