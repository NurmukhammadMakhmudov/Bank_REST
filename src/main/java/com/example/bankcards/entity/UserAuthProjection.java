package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.entity.enums.Status;

public interface UserAuthProjection {
    long getId();
    String getUsername();
    String getPassword();
    Status getStatus();
    Role getRole();
}
