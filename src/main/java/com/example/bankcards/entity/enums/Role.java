package com.example.bankcards.entity.enums;

public enum Role {
    ADMIN(1),
    USER(2);
    private final int roleCode;

    Role(int roleCode) {
        this.roleCode = roleCode;
    }


}
