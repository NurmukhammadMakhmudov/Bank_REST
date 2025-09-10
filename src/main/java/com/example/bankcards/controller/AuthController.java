package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.UserAuthProjection;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.Impl.AuthServiceImpl;
import com.example.bankcards.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceImpl authService;
    private final RefreshTokenService refreshTokenService;
    public AuthController(AuthServiceImpl authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String token = authService.authenticate(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> registerUser(@RequestBody RegistrationRequest request) {
      return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    public JwtResponse refreshToken(@RequestBody RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        UserDetails userDetails = UserPrincipal.create((UserAuthProjection) token.getUser());
        String newAccessToken = authService.authenticate(token.getUser());

        return new JwtResponse(newAccessToken, token.getToken()); // refresh тот же
    }
}
