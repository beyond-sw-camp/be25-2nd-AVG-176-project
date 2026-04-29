package com.example.team3Project.domain.settlement.util;

import com.example.team3Project.domain.settlement.enums.PaymentStatus;

public final class PaymentFailureReasonUtil {

    private PaymentFailureReasonUtil() {
    }

    public static String toKoreanMessage(String paymentStatus) {
        PaymentStatus status = tryParse(paymentStatus);
        if (status == null) {
            return paymentStatus;
        }

        return switch (status) {
            case FAILED_CARD_NOT_FOUND -> "카드를 찾을 수 없습니다.";
            case FAILED_CARD_OWNER_MISMATCH -> "주문자와 카드 소유자가 일치하지 않습니다.";
            case FAILED_CARD_INACTIVE -> "비활성화된 카드입니다.";
            case FAILED_CVC_MISMATCH -> "CVC가 일치하지 않습니다.";
            case FAILED_CARD_EXPIRED -> "만료된 카드입니다.";
            case FAILED_LIMIT_EXCEEDED -> "카드 한도를 초과했습니다.";
            case FAILED_INSUFFICIENT_BALANCE -> "카드 잔액이 부족합니다.";
            case SUCCESS -> null;
        };
    }

    private static PaymentStatus tryParse(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return null;
        }

        try {
            return PaymentStatus.valueOf(paymentStatus);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

