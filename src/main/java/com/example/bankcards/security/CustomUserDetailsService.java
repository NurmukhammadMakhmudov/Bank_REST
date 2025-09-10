package com.example.bankcards.security;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.UserAuthProjection;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAuthProjection user = userRepository.findAuthByUsername(username)
                .orElseThrow(()-> new UsernameNotFoundException("User not found: " + username));
        return UserPrincipal.create(user);
    }
}
