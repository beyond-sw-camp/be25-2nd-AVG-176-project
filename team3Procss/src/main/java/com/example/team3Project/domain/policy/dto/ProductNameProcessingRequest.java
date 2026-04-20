package com.example.team3Project.domain.policy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 상품명 가공 요청 DTO
public class ProductNameProcessingRequest {

    @NotBlank
    private String productName;
}