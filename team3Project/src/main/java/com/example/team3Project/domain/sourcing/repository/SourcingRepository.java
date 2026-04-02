package com.example.team3Project.domain.sourcing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.team3Project.domain.sourcing.entity.Sourcing;

public interface SourcingRepository extends JpaRepository<Sourcing, Long>{

    boolean existsByProductId(String productId);

    boolean existsByProductIdAndUserId(String productId, Long userId);

    Optional<Sourcing> findByProductId(String productId);

    // 이 쿼리가 실행 됨.
    @Query("SELECT DISTINCT s FROM Sourcing s JOIN FETCH s.variations v LEFT JOIN FETCH v.dimensions WHERE s.id = :id")
    Optional<Sourcing> findByIdWithVariations(@Param("id") Long id);
}
