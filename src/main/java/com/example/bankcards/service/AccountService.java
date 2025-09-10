package com.example.bankcards.service;


import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.Currency;
import com.example.bankcards.entity.User;

/**
 * Сервис для работы со счетами пользователей.
 */
public interface AccountService {

    /**
     * Генерирует новый счет для указанного пользователя с выбранной валютой.
     *
     * @param owner   пользователь, которому принадлежит счет
     * @param currency валюта счета
     * @return созданный {@link Account} с уникальным номером и начальными настройками
     */
    Account generateAccount(User owner, Currency currency);
}
