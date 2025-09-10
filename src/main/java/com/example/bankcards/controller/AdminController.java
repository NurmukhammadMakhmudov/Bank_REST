package com.example.bankcards.controller;
import com.example.bankcards.dto.*;
import com.example.bankcards.entity.enums.Status;
import com.example.bankcards.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.bankcards.security.UserPrincipal;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/users/cards")
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CreateCardRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        CardResponse dto = adminService.createCard(req, principal.getId());
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{ownerId}/cards/{cardId}/status")
    public ResponseEntity<CardResponse> updateStatus(
            @PathVariable Long ownerId,
            @PathVariable Long cardId,
            @RequestParam String newStatus,
            @AuthenticationPrincipal UserPrincipal principal) {
        var status = Status.valueOf(newStatus);
        CardResponse dto = adminService.updateCardStatus(ownerId, cardId, status, principal.getId());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/users/{ownerId}/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long ownerId,
                                           @PathVariable Long cardId,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        adminService.removeCard(ownerId, cardId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cards")
    public ResponseEntity<Page<CardResponse>> getAllCards(
            CardFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardResponse> p = adminService.getAllCards(filter, pageable);
        return ResponseEntity.ok(p);
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId,
                                                   @Valid @RequestBody UserUpdateRequest req,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminService.updateUser(userId, req, principal.getId()));
    }


}
