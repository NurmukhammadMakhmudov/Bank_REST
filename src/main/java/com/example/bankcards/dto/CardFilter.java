package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;
import com.example.bankcards.entity.enums.Status;

@Data
@Builder
public class CardFilter {
    private String lastFour;
    private Status status;
    private Long ownerId;
    private String search;
}
