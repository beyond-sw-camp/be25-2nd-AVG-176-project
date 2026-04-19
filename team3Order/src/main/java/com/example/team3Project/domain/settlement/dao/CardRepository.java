package com.example.team3Project.domain.settlement.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserId(Long userId);

    List<Card> findByUserIdOrderByIdAsc(Long userId);

    List<Card> findByUserIdAndActiveTrueOrderByIdAsc(Long userId);

    Optional<Card> findByIdAndUserId(Long id, Long userId);
}
