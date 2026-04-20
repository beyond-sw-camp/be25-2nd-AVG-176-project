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

    @Query("SELECT DISTINCT s FROM Sourcing s LEFT JOIN FETCH s.variations v LEFT JOIN FETCH v.dimensions "
            + "WHERE s.productId = :productId AND s.userId = :userId")
    Optional<Sourcing> findByProductIdAndUserIdWithVariations(
            @Param("productId") String productId,
            @Param("userId") Long userId);
}
