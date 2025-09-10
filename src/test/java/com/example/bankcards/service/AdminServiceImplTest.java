package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.*;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.entity.enums.Status;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.*;
import com.example.bankcards.service.Impl.AdminServiceImpl;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.CardNumberGenerator;
import com.example.bankcards.util.CardUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccountService accountService;
    @Mock private CardNumberGenerator cardNumberGenerator;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private CardMapper cardMapper;

    @InjectMocks private AdminServiceImpl adminService;

    private User owner;
    private Account account;

    @BeforeEach
    void setup() {
        owner = new User();
        owner.setId(42L);
        owner.setUsername("owner42");

        account = new Account();
        account.setId(100L);
        account.setBalance(BigDecimal.ZERO);
    }

    @Test
    void createCard_success() {
        // prepare request
        char[] pin = "1234".toCharArray();
        CreateCardRequest req = new CreateCardRequest(42L, pin);

        // mocks
        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(accountService.generateAccount(owner, Currency.RUB)).thenReturn(account);
        when(cardNumberGenerator.generateCardNumber(account.getId())).thenReturn("4444111122223333");
        // compute hash consistent with CardUtils.sha256Hex
        String hash = CardUtils.sha256Hex("4444111122223333");
        when(cardRepository.findByCardNumberHash(hash)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("hashedPin");

        Card savedCard = new Card();
        savedCard.setId(7L);
        savedCard.setCardNumberHash(hash);
        savedCard.setCardNumberEncrypted("enc");
        savedCard.setLastFourDigits("3333");
        savedCard.setAccount(account);
        savedCard.setUser(owner);
        savedCard.setPassword("hashedPin");
        savedCard.setStatus(Status.ACTIVE);

        when(cardRepository.save(any(Card.class))).thenReturn(savedCard);

        CardResponse dto = CardResponse.builder().id(7L).maskedNumber("**** 3333").build();
        when(cardMapper.toDto(savedCard)).thenReturn(dto);

        // call
        CardResponse res = adminService.createCard(req, 1L);

        // verify
        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(7L);
        assertThat(res.getMaskedNumber()).isEqualTo("**** 3333");

        verify(userRepository).findById(42L);
        verify(accountService).generateAccount(owner, Currency.RUB);
        verify(cardNumberGenerator).generateCardNumber(account.getId());
        verify(cardRepository).findByCardNumberHash(hash);
        verify(passwordEncoder).encode(any(CharSequence.class));
        verify(cardRepository).save(any(Card.class));
        verify(cardMapper).toDto(savedCard);
    }

    @Test
    void createCard_alreadyExists_throwsIllegalArgument() {
        char[] pin = "1234".toCharArray();
        CreateCardRequest req = new CreateCardRequest(42L, pin);

        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(accountService.generateAccount(owner, Currency.RUB)).thenReturn(account);
        when(cardNumberGenerator.generateCardNumber(account.getId())).thenReturn("4444111122223333");
        String hash = CardUtils.sha256Hex("4444111122223333");

        // simulate existing card found
        when(cardRepository.findByCardNumberHash(hash)).thenReturn(Optional.of(new Card()));

        assertThatThrownBy(() -> adminService.createCard(req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card already exists");

        verify(cardRepository).findByCardNumberHash(hash);
        verify(cardRepository, never()).save(any());
    }

    @Test
    void createCard_saveThrowsDataIntegrity_throwsIllegalState() {
        char[] pin = "1234".toCharArray();
        CreateCardRequest req = new CreateCardRequest(42L, pin);

        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(accountService.generateAccount(owner, Currency.RUB)).thenReturn(account);
        when(cardNumberGenerator.generateCardNumber(account.getId())).thenReturn("4444111122223333");
        String hash = CardUtils.sha256Hex("4444111122223333");
        when(cardRepository.findByCardNumberHash(hash)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("hp");

        // simulate DB unique constraint collision during save
        when(cardRepository.save(any(Card.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> adminService.createCard(req, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Card collision on save");

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void updateCardStatus_success() {
        Card card = new Card();
        card.setId(10L);
        User ownerUser = new User(); ownerUser.setId(5L);
        card.setUser(ownerUser);
        card.setStatus(Status.ACTIVE);

        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        CardResponse dto = CardResponse.builder().id(10L).maskedNumber("mask").build();
        when(cardMapper.toDto(card)).thenReturn(dto);

        CardResponse res = adminService.updateCardStatus(5L, 10L, Status.BLOCKED, 99L);

        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(10L);
        assertThat(card.getStatus()).isEqualTo(Status.BLOCKED);
        verify(cardRepository).findById(10L);
        verify(cardMapper).toDto(card);
    }

    @Test
    void updateCardStatus_notOwner_throwsNotFound() {
        Card card = new Card();
        card.setId(10L);
        User ownerUser = new User(); ownerUser.setId(5L);
        card.setUser(ownerUser);

        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> adminService.updateCardStatus(999L, 10L, Status.BLOCKED, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card not found for owner");
    }

    @Test
    void removeCard_positiveBalance_throwsBusinessException() {
        Card card = new Card();
        card.setId(11L);
        User ownerUser = new User(); ownerUser.setId(5L);
        card.setUser(ownerUser);

        Account acct = new Account();
        acct.setId(20L);
        acct.setBalance(new BigDecimal("10.00"));
        card.setAccount(acct);

        when(cardRepository.findById(11L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> adminService.removeCard(5L, 11L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete card with positive balance");

        verify(cardRepository).findById(11L);
        verify(cardRepository, never()).delete(any(Card.class));
    }

    @Test
    void removeCard_success_deletesCardAndDetachAccount() {
        Card card = new Card();
        card.setId(12L);
        User ownerUser = new User(); ownerUser.setId(5L);
        card.setUser(ownerUser);

        Account acct = new Account();
        acct.setId(21L);
        acct.setBalance(BigDecimal.ZERO);
        // ensure account has reference back to card (as remove sets card to null)
        acct.setCard(card);
        card.setAccount(acct);

        when(cardRepository.findById(12L)).thenReturn(Optional.of(card));

        adminService.removeCard(5L, 12L, 1L);

        // card should be deleted and association cleared
        verify(cardRepository).delete(card);
        assertThat(card.getAccount()).isNull();
        assertThat(acct.getCard()).isNull();
    }

    @Test
    void updateUser_success_andUsernameChange() {
        User user = new User();
        user.setId(99L);
        user.setUsername("old");
        user.setFirstName("F");
        user.setLastName("L");

        when(userRepository.findById(99L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newUser")).thenReturn(false);

        UserUpdateRequest req = new UserUpdateRequest(
                "newUser",
                "First",
                "Last",
                "Addr",
                "PS",
                "PN",
                Status.ACTIVE,
                Role.USER
        );

        UserResponse resp = adminService.updateUser(99L, req, 1L);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getUsername()).isEqualTo("newUser");
        assertThat(user.getFirstName()).isEqualTo("First");
        assertThat(user.getLastName()).isEqualTo("Last");
    }

    @Test
    void updateUser_usernameTaken_throws() {
        User user = new User();
        user.setId(99L);
        user.setUsername("old");

        when(userRepository.findById(99L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        UserUpdateRequest req = new UserUpdateRequest(
                "taken",
                null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> adminService.updateUser(99L, req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void getAllCards_withFilter_callsFindAllByUserId() {
        CardFilter filter =  CardFilter.builder().ownerId(5L).build();
        Pageable pageable = PageRequest.of(0, 10);

        Card c = new Card();
        c.setId(1L);
        Page<Card> page = new PageImpl<>(List.of(c), pageable, 1);
        when(cardRepository.findAllByUserId(5L, pageable)).thenReturn(page);
        when(cardMapper.toDto(c)).thenReturn(CardResponse.builder().id(1L).maskedNumber("m").build());

        Page<CardResponse> res = adminService.getAllCards(filter, pageable);

        assertThat(res.getTotalElements()).isEqualTo(1);
        verify(cardRepository).findAllByUserId(5L, pageable);
    }

    @Test
    void getAllCards_noFilter_callsFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Card c = new Card(); c.setId(2L);
        Page<Card> page = new PageImpl<>(List.of(c), pageable, 1);
        when(cardRepository.findAll(pageable)).thenReturn(page);
        when(cardMapper.toDto(c)).thenReturn(CardResponse.builder().id(2L).maskedNumber("m2").build());

        Page<CardResponse> res = adminService.getAllCards(null, pageable);

        assertThat(res.getTotalElements()).isEqualTo(1);
        verify(cardRepository).findAll(pageable);
    }

    @Test
    void getAllUsers_mapsToUserResponseList() {
        User u1 = new User(); u1.setId(1L); u1.setUsername("u1");
        User u2 = new User(); u2.setId(2L); u2.setUsername("u2");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserResponse> list = adminService.getAllUsers();

        assertThat(list).hasSize(2);
        assertThat(list).extracting(UserResponse::getUsername).containsExactlyInAnyOrder("u1", "u2");
        verify(userRepository).findAll();
    }
}
