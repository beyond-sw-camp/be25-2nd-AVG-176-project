package com.example.team3Project.domain.settlement.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardType;   // VISA, MASTER, KAKAO
    private String cardNumber;
    @Column(name = "user_id")
    private Long userId;
    private long balance;

    @Column(name = "card_limit")
    private long cardLimit;

    private boolean active; // 활성화/비활성화

    private String cvcEncrypted;
    private String expiryEncrypted;
}
