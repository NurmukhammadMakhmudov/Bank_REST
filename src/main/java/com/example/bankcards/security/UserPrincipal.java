package com.example.bankcards.security;

import com.example.bankcards.entity.enums.Status;
import com.example.bankcards.entity.UserAuthProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {
    private final long id;
    private final String username;
    private final String password;
    private final Status status;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(UserAuthProjection user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_"+ user.getRole().name()));

        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(), user.getStatus(), authorities);
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public long getId() {return  id;}

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return status.getStatusCode() == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
