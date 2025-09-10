package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.entity.enums.Status;
import lombok.Data;

@Data
public class RegistrationRequest {

    private String username;
    private char[] password;
    private String firstName;
    private String lastName;
    private String address;
    private String passportSeries;
    private String passportNumber;
    private Role role;
    private Status status;


}
