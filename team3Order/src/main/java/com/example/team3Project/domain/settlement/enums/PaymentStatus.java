package com.example.team3Project.domain.settlement.enums;

public enum PaymentStatus {

    SUCCESS,
    FAILED_CARD_NOT_FOUND,
    FAILED_CARD_OWNER_MISMATCH,
    FAILED_CARD_INACTIVE,
    FAILED_CVC_MISMATCH,
    FAILED_CARD_EXPIRED,
    FAILED_LIMIT_EXCEEDED,
    FAILED_INSUFFICIENT_BALANCE
}
