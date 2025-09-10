package com.example.bankcards.dto;


import lombok.Data;

@Data
public class CreateCardRequest {

    private final long ownerId;
    private final char[] password;

}
