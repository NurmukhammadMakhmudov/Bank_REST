package com.example.bankcards.util;

import com.example.bankcards.repository.AccountRepository;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardNumberGenerator {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    @Value("app.country.bin")
    private static String BIN;

    public String generateCardNumber(Long accountId) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        String accountPart = String.format("%09d", account.getId());

        String partialNumber = BIN + accountPart;

        int checkDigit = getCheckDigit(partialNumber);
        return partialNumber + checkDigit;
    }

    private int getCheckDigit(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {
            int digit = Character.getNumericValue(number.charAt(i));
            if ((number.length() - i) % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }
}
