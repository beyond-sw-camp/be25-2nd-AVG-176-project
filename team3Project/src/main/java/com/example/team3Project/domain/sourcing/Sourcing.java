package com.example.team3Project.domain.sourcing;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sourcing{

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    private String sourceUrl;
    private String siteName;
    private String productId;
    private String collectedAt;

    // 여기는 data
    private String title;
    private BigDecimal originalPrice;
    private String currency;
    private String brand;
    private String mainImageUrl;
    private String stockStatus;

}