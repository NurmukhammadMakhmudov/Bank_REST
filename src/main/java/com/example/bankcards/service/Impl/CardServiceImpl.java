package com.example.bankcards.service.Impl;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.Status;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.CardUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;
    private final CardMapper cardMapper;
    private final UserRepository userRepository;


    @Override
    public Page<CardResponse> getListOfCards(Long requesterId, CardFilter filter, Pageable pageable) {
        Long ownerId = (filter != null && filter.getOwnerId() != null) ? filter.getOwnerId() : requesterId;
        log.debug("getListOfCards requesterId={}, ownerId={}, pageable={}", requesterId, ownerId, pageable);
        Page<Card> page = cardRepository.findAllByUserId(ownerId, pageable);
        Page<CardResponse> dtoPage = page.map(cardMapper::toDto);
        log.info("getListOfCards returned {} items (totalElements={}) for ownerId={}", dtoPage.getContent().size(), dtoPage.getTotalElements(), ownerId);
        return dtoPage;
    }


    @Override
    public void requestCardBlock(Long requesterId, Long cardId) {
        log.debug("requestCardBlock requesterId={}, cardId={}", requesterId, cardId);
        Card card = cardRepository.findById(cardId).orElseThrow(() -> {
            log.warn("requestCardBlock: card {} not found", cardId);
            return new NotFoundException("Card not found");
        });
        if (!card.getUser().getId().equals(requesterId)) {
            log.warn("requestCardBlock: user {} is not owner of card {}", requesterId, cardId);
            throw new AccessDeniedException("Not owner");
        }
        card.setStatus(Status.BLOCKED);
        log.info("requestCardBlock: card {} blocked by user {}", cardId, requesterId);
    }

    @Override
    @Transactional
    public TransferResponse transferMoney(Long requesterId, TransferRequest request) {
        log.debug("transferMoney requesterId={}, request={}", requesterId, request);

        // Нормализация входных номеров
        if (request.getFromCardNumber() == null || request.getToCardNumber() == null) {
            throw new IllegalArgumentException("fromCardNumber and toCardNumber are required");
        }
        String fromNumber = request.getFromCardNumber().replaceAll("\\s+", "");
        String toNumber = request.getToCardNumber().replaceAll("\\s+", "");

        // Не разрешаем перевод самому себе (можно убрать, если нужно разрешить)
        if (fromNumber.equals(toNumber)) {
            log.warn("transferMoney: from and to card numbers are identical ({}).", fromNumber);
            throw new BusinessException("Cannot transfer to the same card");
        }

        // Разрешаем номера в сущности Card (вспомогательный метод)
        Card from = resolveCardByNumber(fromNumber);
        Card to   = resolveCardByNumber(toNumber);

        log.debug("transferMoney: resolved fromId={} toId={}", from.getId(), to.getId());

        // Проверка владения — перевод только между картами одного пользователя (по требованию)
        if (!Objects.equals(from.getUser().getId(), requesterId) || !Objects.equals(to.getUser().getId(), requesterId)) {
            log.warn("transferMoney: user {} attempted transfer between cards {} -> {} not owned by them", requesterId, from.getId(), to.getId());
            throw new AccessDeniedException("Can transfer only between your cards");
        }

        // Статусы карт
        if (from.getStatus() != Status.ACTIVE || to.getStatus() != Status.ACTIVE) {
            log.warn("transferMoney: one of cards not ACTIVE (from.status={}, to.status={})", from.getStatus(), to.getStatus());
            throw new BusinessException("One of cards is not active");
        }

        // Проверка баланса
        if (from.getAccount().getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("transferMoney: insufficient funds: cardId={}, balance={}, requested={}", from.getId(), from.getAccount().getBalance(), request.getAmount());
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Здесь можно использовать блокировку PESSIMISTIC_WRITE для from/to, если репозиторий поддерживает:
        // Card from = cardRepository.findByCardNumberHashForUpdate(fromHash).orElseThrow(...);
        // Card to   = cardRepository.findByCardNumberHashForUpdate(toHash).orElseThrow(...);
        // Это защитит от race condition при одновременных переводах.

        // Обновляем балансы и сохраняем (в одной транзакции)
        from.getAccount().setBalance(from.getAccount().getBalance().subtract(request.getAmount()));
        to.getAccount().setBalance(to.getAccount().getBalance().add(request.getAmount()));

        cardRepository.save(from);
        cardRepository.save(to);

        // Сохраняем запись в истории переводов
        Transfer t = new Transfer();
        t.setFromCard(from);
        t.setToCard(to);
        t.setAmount(request.getAmount());
        t.setComment(request.getComment());
        t.setCreatedAt(LocalDateTime.now());
        t = transferRepository.save(t);

        log.info("transferMoney: user {} transferred {} from card {} to card {} (transferId={})",
                requesterId, request.getAmount(), from.getId(), to.getId(), t.getId());

        return TransferResponse.builder()
                .id(t.getId())
                .fromCardId(from.getId())
                .toCardId(to.getId())
                .amount(t.getAmount())
                .createdAt(t.getCreatedAt())
                .fromBalanceAfter(from.getAccount().getBalance())
                .toBalanceAfter(to.getAccount().getBalance())
                .comment(t.getComment())
                .build();
    }

    /**
     * Вспомогательный метод — разрешает cardNumber -> Card или бросает NotFoundException.
     * Важно: нормализует номер, вычисляет hash через CardUtils и использует репозиторий.
     */
    private Card resolveCardByNumber(String cardNumber) {
        String normalized = cardNumber.replaceAll("\\s+", "");
        String hash = CardUtils.sha256Hex(normalized);
        return cardRepository.findByCardNumberHash(hash)
                .orElseThrow(() -> {
                    log.warn("resolveCardByNumber: card not found for hash={}", hash);
                    return new NotFoundException("Card not found");
                });
    }


    @Override
    public BalanceResponse checkBalance(Long requesterId, Long cardId) {
        log.debug("checkBalance requesterId={}, cardId={}", requesterId, cardId);
        Card card = cardRepository.findById(cardId).orElseThrow(() -> {
            log.warn("checkBalance: card {} not found", cardId);
            return new NotFoundException("Card not found");
        });
        if (!card.getUser().getId().equals(requesterId)) {
            log.warn("checkBalance: user {} is not owner of card {}", requesterId, cardId);
            throw new AccessDeniedException("Not owner");
        }
        BalanceResponse resp = BalanceResponse.builder()
                .cardMask("**** **** **** " + card.getLastFourDigits())
                .balance(card.getAccount().getBalance())
                .build();
        log.info("checkBalance: user {} checked balance for card {} -> {}", requesterId, cardId, resp.getBalance());
        return resp;
    }
}
