package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Сервис для управления банковскими картами.
 * Реализует бизнес-логику работы с картами, блокировки, перевода средств и проверки баланса.
 */
public interface CardService {

    /**
     * Возвращает список карт для указанного пользователя.
     *
     * @param requesterId ID пользователя, делающего запрос
     * @param filter      фильтры поиска (по владельцу и др.)
     * @param pageable    параметры пагинации
     * @return страница с картами
     */
    Page<CardResponse> getListOfCards(Long requesterId, CardFilter filter, Pageable pageable);

    /**
     * Помечает карту как заблокированную.
     *
     * @param requesterId ID пользователя, делающего запрос
     * @param cardId      ID карты
     * @throws org.springframework.security.access.AccessDeniedException если пользователь не владелец карты
     */
    void requestCardBlock(Long requesterId, Long cardId);

    /**
     * Перевод средств между картами одного пользователя.
     *
     * @param requesterId ID пользователя
     * @param request     данные перевода (номер карт, сумма, комментарий)
     * @return результат перевода (балансы после операции)
     * @throws com.example.bankcards.exception.InsufficientFundsException если недостаточно средств
     * @throws com.example.bankcards.exception.BusinessException          если перевод невозможен
     */
    TransferResponse transferMoney(Long requesterId, TransferRequest request);

    /**
     * Проверка текущего баланса карты.
     *
     * @param requesterId ID пользователя
     * @param cardId      ID карты
     * @return баланс карты
     * @throws org.springframework.security.access.AccessDeniedException если пользователь не владелец карты
     */
    BalanceResponse checkBalance(Long requesterId, Long cardId);
}
