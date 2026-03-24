package com.example.team3Project.domain.sourcing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team3Project.domain.sourcing.entity.SourcingVariation;

public interface SourcingVariationRepository extends JpaRepository<SourcingVariation, Long> {
}
