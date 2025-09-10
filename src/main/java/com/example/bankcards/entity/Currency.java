package com.example.bankcards.entity;

public enum Currency {
    USD(1),
    RUB(2);

    private final int currencyCode;

    Currency(int currencyCode) {
        this.currencyCode = currencyCode;
    }
}
