package com.example.team3Project.domain.product.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourcingRepository extends JpaRepository<Sourcing, Long> {
    List<Sourcing> findByProductId(String productId);
}
