package com.example.team3Project.domain.settlement.dto;

import lombok.Data;

@Data
public class CardRequest {

    private Long userId;

    private String cardType;

    private String cardNumber;

    private Long balance;

    private Long cardLimit;

    private String cvc;

    private String expiry;
}
