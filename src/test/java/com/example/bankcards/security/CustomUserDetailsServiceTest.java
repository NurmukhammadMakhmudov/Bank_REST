package com.example.bankcards.security;

import com.example.bankcards.entity.UserAuthProjection;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails_viaUserPrincipalStaticCreate() {
        String username = "alice";
        UserAuthProjection proj = mock(UserAuthProjection.class);
        when(userRepository.findAuthByUsername(username)).thenReturn(Optional.of(proj));

        // создаём мок UserPrincipal (имплементирует UserDetails в твоём коде)
        UserPrincipal upMock = mock(UserPrincipal.class);
        when(upMock.getUsername()).thenReturn("alice");
        when(upMock.getPassword()).thenReturn("pass");
        // если UserPrincipal.getAuthorities() используется — замокай их тоже при необходимости

        // статический мок возвращает UserPrincipal (НЕ org.springframework.security.core.userdetails.User)
        try (MockedStatic<UserPrincipal> utilities = mockStatic(UserPrincipal.class)) {
            utilities.when(() -> UserPrincipal.create(proj)).thenReturn(upMock);

            UserDetails ud = service.loadUserByUsername(username);

            assertThat(ud).isNotNull();
            // теперь ud == upMock
            assertThat(ud).isSameAs(upMock);
            assertThat(ud.getUsername()).isEqualTo("alice");
            assertThat(ud.getPassword()).isEqualTo("pass");

            utilities.verify(() -> UserPrincipal.create(proj));
        }

        verify(userRepository).findAuthByUsername(username);
    }


    @Test
    void loadUserByUsername_notFound_throws() {
        String username = "noone";
        when(userRepository.findAuthByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(userRepository).findAuthByUsername(username);
    }
}
