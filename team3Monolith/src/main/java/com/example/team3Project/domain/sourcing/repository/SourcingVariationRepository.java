package com.example.team3Project.domain.sourcing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team3Project.domain.sourcing.entity.SourcingVariation;

public interface SourcingVariationRepository extends JpaRepository<SourcingVariation, Long> {

    List<SourcingVariation> findBySourcingId(Long sourcingId);
}
