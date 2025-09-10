package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
public class BalanceResponse {
    private final String cardMask;
    private final BigDecimal balance;
}
