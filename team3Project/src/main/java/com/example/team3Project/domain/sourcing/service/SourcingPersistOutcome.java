package com.example.team3Project.domain.sourcing.service;

import com.example.team3Project.domain.sourcing.entity.SourcingRegistrationStatus;

// 소싱 및 정규화 결과를 한번에 넘기기 위해 사용하는 것.
public record SourcingPersistOutcome(
        Long sourcingId,
        SourcingRegistrationStatus registrationStatus,
        String normalizationErrorMessage
) {
}
