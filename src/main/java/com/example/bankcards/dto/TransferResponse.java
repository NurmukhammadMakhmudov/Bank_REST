package com.example.bankcards.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class TransferResponse {
    private Long id;
    private Long fromCardId;
    private Long toCardId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;
    private String comment;
}
