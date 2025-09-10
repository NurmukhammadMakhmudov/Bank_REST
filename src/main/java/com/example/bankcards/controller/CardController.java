package com.example.bankcards.controller;
import com.example.bankcards.dto.*;
import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.bankcards.security.UserPrincipal;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<Page<CardResponse>> myCards(
            CardFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardResponse> p = cardService.getListOfCards(principal.getId(), filter, pageable);
        return ResponseEntity.ok(p);
    }
    
    @PostMapping("/{cardId}/block")
    public ResponseEntity<Void> requestBlock(@PathVariable Long cardId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        cardService.requestCardBlock(principal.getId(), cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest req,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        TransferResponse resp = cardService.transferMoney(principal.getId(), req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BalanceResponse> balance(@PathVariable Long cardId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cardService.checkBalance(principal.getId(), cardId));
    }
}
