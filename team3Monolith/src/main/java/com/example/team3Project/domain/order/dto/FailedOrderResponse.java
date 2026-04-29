package com.example.team3Project.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FailedOrderResponse {
    private Long id;
    private Long userId;
    private String customerName;
    private String customerPhone;
    private int totalAmount;
    private String status;
    private String autoOrderStatus;
    private String paymentFailureCode;
    private String paymentFailureReason;
    private Long cardId;
    private String cardType;
    private String cardLast4;
    private Boolean cardActive;
    private Long cardLimit;
    private Long cardBalance;
    private Long configuredLimit;
}
