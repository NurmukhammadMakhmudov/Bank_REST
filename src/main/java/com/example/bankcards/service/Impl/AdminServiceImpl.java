package com.example.bankcards.service.Impl;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.*;
import com.example.bankcards.entity.enums.Status;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.*;
import com.example.bankcards.service.AccountService;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.CardNumberGenerator;
import com.example.bankcards.util.CardUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final CardNumberGenerator cardNumberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final CardMapper cardMapper;

    @Override
    @Transactional
    @Retryable(
            retryFor = DataIntegrityViolationException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 200)
    )
    public CardResponse createCard(CreateCardRequest request, Long performedById) {
        log.debug("createCard called: ownerId={}, performedById={}", request.getOwnerId(), performedById);

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> {
                    log.warn("createCard: User {} not found", request.getOwnerId());
                    return new NotFoundException("User not found");
                });

        Account account = accountService.generateAccount(owner, Currency.RUB);

        String cardNumber = cardNumberGenerator.generateCardNumber(account.getId());
        String cardNumberHash = CardUtils.sha256Hex(cardNumber);
        String cardNumberEncrypted = CardUtils.encrypt(cardNumber);

        char[] pinChars = request.getPassword();
        String pinHash;
        try {
            pinHash = passwordEncoder.encode(CharBuffer.wrap(pinChars));
        } finally {
            Arrays.fill(pinChars, '\0');
        }

        if (cardRepository.findByCardNumberHash(cardNumberHash).isPresent()) {
            log.warn("createCard: Card collision for hash={}", cardNumberHash);
            throw new IllegalArgumentException("Card already exists");
        }

        Card card = new Card();
        card.setCardNumberEncrypted(cardNumberEncrypted);
        card.setCardNumberHash(cardNumberHash);
        card.setPassword(pinHash);
        card.setAccount(account);
        card.setStatus(Status.ACTIVE);
        card.setLastFourDigits(CardUtils.last4(cardNumber));
        card.setUser(owner);

        card = cardRepository.save(card);
        log.info("createCard: Card {} created for user {} by admin {}", card.getId(), owner.getId(), performedById);

        return cardMapper.toDto(card);
    }

    @Recover
    public CardResponse recover(DataIntegrityViolationException ex, Long ownerId, CreateCardRequest request, Long performedById) {
        log.error("recover: Failed to create unique card for ownerId={} after retries", ownerId, ex);
        throw new IllegalStateException("Не удалось сгенерировать уникальный номер карты после всех попыток", ex);
    }

    @Override
    @Transactional
    public CardResponse updateCardStatus(Long ownerId, Long cardId, Status newStatus, Long performedById) {
        log.debug("updateCardStatus called: cardId={}, ownerId={}, newStatus={}, performedById={}", cardId, ownerId, newStatus, performedById);
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("updateCardStatus: card {} not found", cardId);
                    return new NotFoundException("Card not found");
                });

        if (!card.getUser().getId().equals(ownerId)) {
            log.warn("updateCardStatus: card {} does not belong to owner {}", cardId, ownerId);
            throw new NotFoundException("Card not found for owner");
        }

        card.setStatus(newStatus);
        log.info("updateCardStatus: card {} status changed to {} by admin {}", cardId, newStatus, performedById);
        return cardMapper.toDto(card);
    }

    @Transactional
    public void removeCard(Long ownerId, Long cardId, Long performedById) {
        log.debug("removeCard called: cardId={}, ownerId={}, performedById={}", cardId, ownerId, performedById);
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("removeCard: card {} not found", cardId);
                    return new NotFoundException("Card not found");
                });

        if (!card.getUser().getId().equals(ownerId)) {
            log.warn("removeCard: card {} does not belong to owner {}", cardId, ownerId);
            throw new NotFoundException("Card not found for owner");
        }

        if (card.getAccount() != null && card.getAccount().getBalance().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("removeCard: card {} has positive balance", cardId);
            throw new BusinessException("Cannot delete card with positive balance");
        }

        Account account = card.getAccount();
        if (account != null) {
            account.setCard(null);
            card.setAccount(null);
        }

        cardRepository.delete(card);
        log.info("removeCard: card {} deleted by admin {}", cardId, performedById);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request, Long performedById) {
        log.debug("updateUser called: userId={}, performedById={}", userId, performedById);
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("updateUser: user {} not found", userId);
            return new NotFoundException("User not found");
        });

        // Обновление полей
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getPassportNumber() != null) user.setPassportNumber(request.getPassportNumber());
        if (request.getPassportSeries() != null) user.setPassportSeries(request.getPassportSeries());

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("updateUser: username {} already exists", request.getUsername());
                throw new IllegalArgumentException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        log.info("updateUser: user {} updated by admin {}", userId, performedById);
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .address(user.getAddress())
                .passportSeries(user.getPassportSeries())
                .passportNumber(user.getPassportNumber())
                .status(user.getStatus())
                .build();
    }

    @Override
    public Page<CardResponse> getAllCards(CardFilter filter, Pageable pageable) {
        log.debug("getAllCards called: filter={}, pageable={}", filter, pageable);
        Page<CardResponse> result;
        if (filter != null && filter.getOwnerId() != null) {
            result = cardRepository.findAllByUserId(filter.getOwnerId(), pageable).map(cardMapper::toDto);
        } else {
            result = cardRepository.findAll(pageable).map(cardMapper::toDto);
        }
        log.info("getAllCards returned {} cards", result.getTotalElements());
        return result;
    }

    @Override
    public List<UserResponse> getAllUsers() {
        log.debug("getAllUsers called");
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .address(u.getAddress())
                        .passportSeries(u.getPassportSeries())
                        .passportNumber(u.getPassportNumber())
                        .status(u.getStatus())
                        .build())
                .toList();
        log.info("getAllUsers returned {} users", users.size());
        return users;
    }
}
