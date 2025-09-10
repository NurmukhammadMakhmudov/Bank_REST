package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@Builder
public class CardResponse {

    private final long id;
    private final LocalDateTime creationAt;
    private final LocalDate expirationDate;
    private final Status  status;
    private final String maskedNumber;
    private final long cardHolderId;
    private final BigDecimal balance;


}
