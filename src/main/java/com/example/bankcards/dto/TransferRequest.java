package com.example.bankcards.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {


    private String fromCardNumber;
    private String toCardNumber;

    @NotNull @Positive
    private BigDecimal amount;

    private String comment;
}
