package com.example.team3Project.domain.settlement.dto;

import lombok.Data;

@Data
public class CardResponse {

    private Long id;
    private Long userId;
    private Integer paymentPriority;

    private String cardType;

    private String cardNumber;

    private Long balance;

    private Long cardLimit;

    private boolean active;

    private String cvc;

    private String expiry;
}
