package com.example.team3Project.domain.settlement.dto;

import lombok.Data;

@Data
public class PaymentRequest {

    private Long orderId;
    private Long cardId;

}
