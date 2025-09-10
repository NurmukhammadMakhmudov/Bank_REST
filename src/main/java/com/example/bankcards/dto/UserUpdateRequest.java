package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.entity.enums.Status;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String address;
    private final String passportSeries;
    private final String passportNumber;
    private final Status status;
    private final Role role;
}
