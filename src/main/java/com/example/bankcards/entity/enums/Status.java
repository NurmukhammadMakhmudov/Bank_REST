package com.example.bankcards.entity.enums;

public enum Status {
    ACTIVE(1),
    BLOCKED(2),
    SUSPENDED(3);

    private final int statusCode;


    Status(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
