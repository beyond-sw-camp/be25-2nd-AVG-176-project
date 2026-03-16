package com.example.team3Project.domain.sourcing;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SourcingRepository extends JpaRepository<Sourcing, Long>{

    boolean existsByProductId(String productId);
    Optional<Sourcing> findByProductId(String productId);
}
