package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Сервис для административных операций: управление картами и пользователями.
 */
public interface AdminService {

    /**
     * Создает новую карту для пользователя.
     *
     * @param request       данные для создания карты
     * @param performedById ID администратора, выполняющего операцию
     * @return созданный объект {@link CardResponse}
     */
    CardResponse createCard(CreateCardRequest request, Long performedById);

    /**
     * Обновляет статус карты указанного пользователя.
     *
     * @param ownerId       ID владельца карты
     * @param cardId        ID карты
     * @param newStatus     новый статус карты {@link Status} (ACTIVE, BLOCKED, EXPIRED)
     * @param performedById ID администратора, выполняющего операцию
     * @return обновленный объект {@link CardResponse}
     */
    CardResponse updateCardStatus(Long ownerId, Long cardId, Status newStatus, Long performedById);

    /**
     * Удаляет карту указанного пользователя.
     *
     * @param ownerId       ID владельца карты
     * @param cardId        ID карты
     * @param performedById ID администратора, выполняющего операцию
     */
    void removeCard(Long ownerId, Long cardId, Long performedById);

    /**
     * Обновляет данные пользователя.
     *
     * @param userId        ID пользователя
     * @param request       данные для обновления {@link UserUpdateRequest}
     * @param performedById ID администратора, выполняющего операцию
     * @return обновленный объект {@link UserResponse}
     */
    UserResponse updateUser(Long userId, UserUpdateRequest request, Long performedById);

    /**
     * Возвращает список всех карт с возможностью фильтрации и пагинации.
     *
     * @param filter   фильтры поиска {@link CardFilter}
     * @param pageable параметры постраничного вывода {@link Pageable}
     * @return страница объектов {@link CardResponse}
     */
    Page<CardResponse> getAllCards(CardFilter filter, Pageable pageable);

    /**
     * Возвращает список всех пользователей системы.
     *
     * @return список объектов {@link UserResponse}
     */
    List<UserResponse> getAllUsers();
}
