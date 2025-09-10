package com.example.bankcards.util;


import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {
    public CardResponse toDto(Card c) {
        return CardResponse.builder()
                .id(c.getId())
                .maskedNumber("**** **** **** " + c.getCardNumberEncrypted())
                .expirationDate(c.getExpirationDate())
                .status(c.getStatus())
                .balance(c.getAccount().getBalance())
                .cardHolderId(c.getUser().getId())
                .creationAt(c.getCreatedAt())
                .build();
    }
}
