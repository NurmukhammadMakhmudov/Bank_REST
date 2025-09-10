package com.example.bankcards.security;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Date;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwt;

    @BeforeEach
    void setUp() throws Exception {
        jwt = new JwtTokenProvider();

        // Устанавливаем приватные поля через reflection
        // Секрет должен быть не короче 32 байт для HS256 (256 bits)
        String secret = "01234567890123456789012345678901"; // 32 chars
        long expirationMs = 1000L * 60 * 60; // 1 hour

        Field fSecret = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        fSecret.setAccessible(true);
        fSecret.set(jwt, secret);

        Field fExp = JwtTokenProvider.class.getDeclaredField("jwtExpirationMs");
        fExp.setAccessible(true);
        fExp.setLong(jwt, expirationMs);
    }

    @Test
    void generateAndValidateToken_success() {
        UserDetails user = User.withUsername("alice")
                .password("pwd")
                .roles("USER")
                .build();

        String token = jwt.generateAccessToken(user);
        assertThat(token).isNotBlank();

        boolean valid = jwt.validateToken(token);
        assertThat(valid).isTrue();

        String username = jwt.getUsernameFromToken(token);
        assertThat(username).isEqualTo("alice");

        Date exp = jwt.getExpiryDateFromToken(token);
        assertThat(exp).isAfterOrEqualTo(new Date());
    }

    @Test
    void validateToken_tamperedToken_false() {
        UserDetails user = User.withUsername("bob").password("x").roles("USER").build();
        String token = jwt.generateAccessToken(user);
        // simple tamper: change last char
        String bad = token.substring(0, token.length() - 1) + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        boolean ok = jwt.validateToken(bad);
        assertThat(ok).isFalse();
    }

    @Test
    void validateToken_expired_false() throws Exception {
        // set very short expiration
        Field fExp = JwtTokenProvider.class.getDeclaredField("jwtExpirationMs");
        fExp.setAccessible(true);
        fExp.setLong(jwt, 50L); // 50 ms

        UserDetails user = User.withUsername("tim").password("p").roles("USER").build();
        String token = jwt.generateAccessToken(user);

        // wait until expired
        Thread.sleep(120);

        assertThat(jwt.validateToken(token)).isFalse();
    }
}
