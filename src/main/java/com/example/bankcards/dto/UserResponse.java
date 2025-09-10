package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.Status;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private final long id;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String address;
    private final String passportSeries;
    private final String passportNumber;
    private final Status status;

}
