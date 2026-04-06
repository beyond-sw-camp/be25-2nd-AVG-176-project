package com.example.team3Project.domain.sourcing.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team3Project.domain.sourcing.DTO.SourcingProductResponse;
import com.example.team3Project.domain.sourcing.entity.Sourcing;
import com.example.team3Project.domain.sourcing.repository.SourcingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SourcingProductQueryService {

    private final SourcingRepository sourcingRepository;

    @Transactional(readOnly = true)
    public Optional<SourcingProductResponse> findByAsinForUser(String asin, Long userId) {
        if (asin == null || asin.isBlank() || userId == null) {
            return Optional.empty();
        }
        return sourcingRepository
                .findByProductIdAndUserIdWithVariations(asin.trim(), userId)
                .map(SourcingProductResponse::fromEntity);
    }
}
