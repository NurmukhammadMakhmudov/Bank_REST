package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Card> findByCardNumberHash(String hash);
    Optional<Card> findByIdAndUserId(Long id, Long ownerId);
    Page<Card> findAllByUserId(Long userId, Pageable pageable);
}
