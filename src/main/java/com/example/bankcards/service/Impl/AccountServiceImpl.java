package com.example.bankcards.service.Impl;

import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.Currency;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.AccountRepository;
import com.example.bankcards.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Account generateAccount(User owner, Currency currency) {
        log.debug("generateAccount called: ownerId={}, currency={}", owner.getId(), currency);
        Account account = new Account();
        account.setUser(owner);
        account.setCurrency(currency);
        account = accountRepository.save(account);
        log.info("generateAccount: account {} created for user {}", account.getId(), owner.getId());
        return account;
    }
}

