package com.example.team3Project.domain.settlement.dto;

import lombok.Data;

@Data
public class DecryptedCardInfo {

    private String cardNumber;
    private String cvc;
    private String expiry;

    private Long balance;
    private Long cardLimit;
}
