package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.*;
import com.example.bankcards.entity.enums.Status;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.*;
import com.example.bankcards.service.Impl.CardServiceImpl;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.CardUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock private CardRepository cardRepository;
    @Mock private TransferRepository transferRepository;
    @Mock private CardMapper cardMapper;
    @Mock private UserRepository userRepository;

    @InjectMocks private CardServiceImpl cardService;

    private User user;
    private Account accFrom;
    private Account accTo;
    private Card cardFrom;
    private Card cardTo;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");

        accFrom = new Account();
        accFrom.setId(10L);
        accFrom.setBalance(new BigDecimal("1000.00"));

        accTo = new Account();
        accTo.setId(11L);
        accTo.setBalance(new BigDecimal("100.00"));

        cardFrom = new Card();
        cardFrom.setId(100L);
        cardFrom.setUser(user);
        cardFrom.setAccount(accFrom);
        cardFrom.setStatus(Status.ACTIVE);
        cardFrom.setCardNumberEncrypted("enc-from-1111");
        cardFrom.setLastFourDigits("1111");
        // set hash corresponding to plain number "4444111122223333"
        cardFrom.setCardNumberHash(CardUtils.sha256Hex("4444111122223333"));

        cardTo = new Card();
        cardTo.setId(200L);
        cardTo.setUser(user);
        cardTo.setAccount(accTo);
        cardTo.setStatus(Status.ACTIVE);
        cardTo.setCardNumberEncrypted("enc-to-2222");
        cardTo.setLastFourDigits("2222");
        cardTo.setCardNumberHash(CardUtils.sha256Hex("5555222233334444"));
    }

    @Test
    void getListOfCards_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> repoPage = new PageImpl<>(List.of(cardFrom, cardTo), pageable, 2);
        when(cardRepository.findAllByUserId(eq(1L), eq(pageable))).thenReturn(repoPage);

        CardResponse respFrom = CardResponse.builder().id(100L).maskedNumber("**** 1111").build();
        CardResponse respTo = CardResponse.builder().id(200L).maskedNumber("**** 2222").build();
        when(cardMapper.toDto(cardFrom)).thenReturn(respFrom);
        when(cardMapper.toDto(cardTo)).thenReturn(respTo);

        Page<CardResponse> result = cardService.getListOfCards(1L, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactly(respFrom, respTo);
        verify(cardRepository).findAllByUserId(1L, pageable);
    }

    @Test
    void requestCardBlock_success_changesStatus() {
        when(cardRepository.findById(100L)).thenReturn(Optional.of(cardFrom));

        cardService.requestCardBlock(1L, 100L);

        assertThat(cardFrom.getStatus()).isEqualTo(Status.BLOCKED);
        // Current implementation does not call save() in requestCardBlock, so verify save never happens:
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void requestCardBlock_notFound_throwsNotFound() {
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cardService.requestCardBlock(1L, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void requestCardBlock_notOwner_throwsAccessDenied() {
        User other = new User(); other.setId(2L);
        cardFrom.setUser(other);
        when(cardRepository.findById(100L)).thenReturn(Optional.of(cardFrom));

        assertThatThrownBy(() -> cardService.requestCardBlock(1L, 100L))
                .isInstanceOf(AccessDeniedException.class);
        // status should remain unchanged (was ACTIVE)
        assertThat(cardFrom.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void transferMoney_success_byCardNumbers() {
        TransferRequest req = new TransferRequest();
        req.setFromCardNumber("4444 1111 2222 3333");
        req.setToCardNumber("5555 2222 3333 4444");
        req.setAmount(new BigDecimal("200.00"));
        req.setComment("payment");

        // mock resolution by hash
        when(cardRepository.findByCardNumberHash(eq(CardUtils.sha256Hex("4444111122223333"))))
                .thenReturn(Optional.of(cardFrom));
        when(cardRepository.findByCardNumberHash(eq(CardUtils.sha256Hex("5555222233334444"))))
                .thenReturn(Optional.of(cardTo));

        // transfer saves
        Transfer saved = new Transfer();
        saved.setId(500L);
        saved.setFromCard(cardFrom);
        saved.setToCard(cardTo);
        saved.setAmount(req.getAmount());
        saved.setCreatedAt(LocalDateTime.now());
        saved.setComment(req.getComment());
        when(transferRepository.save(any(Transfer.class))).thenReturn(saved);

        TransferResponse resp = cardService.transferMoney(1L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(500L);
        assertThat(resp.getFromBalanceAfter()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(resp.getToBalanceAfter()).isEqualByComparingTo(new BigDecimal("300.00"));

        verify(cardRepository).save(cardFrom);
        verify(cardRepository).save(cardTo);
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    void transferMoney_sameCard_throwsBusinessException() {
        TransferRequest req = new TransferRequest();
        req.setFromCardNumber("4444111122223333");
        req.setToCardNumber("4444111122223333");
        req.setAmount(new BigDecimal("10.00"));

        assertThatThrownBy(() -> cardService.transferMoney(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot transfer to the same card");
    }

    @Test
    void transferMoney_insufficientFunds_throws() {
        TransferRequest req = new TransferRequest();
        req.setFromCardNumber("4444 1111 2222 3333");
        req.setToCardNumber("5555 2222 3333 4444");
        req.setAmount(new BigDecimal("2000.00"));

        when(cardRepository.findByCardNumberHash(anyString())).thenReturn(Optional.of(cardFrom));

        assertThatThrownBy(() -> cardService.transferMoney(1L, req))
                .isInstanceOf(InsufficientFundsException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void transferMoney_notOwner_throwsAccessDenied() {
        // make target card belong to other user
        User other = new User(); other.setId(2L);
        cardTo.setUser(other);

        TransferRequest req = new TransferRequest();
        req.setFromCardNumber("4444111122223333");
        req.setToCardNumber("5555222233334444");
        req.setAmount(new BigDecimal("10.00"));

        when(cardRepository.findByCardNumberHash(eq(CardUtils.sha256Hex("4444111122223333"))))
                .thenReturn(Optional.of(cardFrom));
        when(cardRepository.findByCardNumberHash(eq(CardUtils.sha256Hex("5555222233334444"))))
                .thenReturn(Optional.of(cardTo));

        assertThatThrownBy(() -> cardService.transferMoney(1L, req))
                .isInstanceOf(AccessDeniedException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void transferMoney_fromCardNotFound_throwsNotFound() {
        TransferRequest req = new TransferRequest();
        req.setFromCardNumber("0000 0000 0000 0000");
        req.setToCardNumber("5555 2222 3333 4444");
        req.setAmount(new BigDecimal("1.00"));

        when(cardRepository.findByCardNumberHash(eq(CardUtils.sha256Hex("0000000000000000"))))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.transferMoney(1L, req))
                .isInstanceOf(NotFoundException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void checkBalance_success_and_mask_lastFour() {
        when(cardRepository.findById(100L)).thenReturn(Optional.of(cardFrom));

        BalanceResponse resp = cardService.checkBalance(1L, 100L);

        assertThat(resp).isNotNull();
        assertThat(resp.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        // Expect mask to include last four digits
        assertThat(resp.getCardMask()).contains("1111");
    }

    @Test
    void checkBalance_notOwner_throws() {
        User other = new User(); other.setId(2L);
        cardFrom.setUser(other);
        when(cardRepository.findById(100L)).thenReturn(Optional.of(cardFrom));

        assertThatThrownBy(() -> cardService.checkBalance(1L, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
