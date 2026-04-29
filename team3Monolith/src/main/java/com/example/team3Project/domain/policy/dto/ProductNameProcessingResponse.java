package com.example.team3Project.domain.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 상품명 가공 응답 DTO
public class ProductNameProcessingResponse {
    private final boolean excluded; // 해당 상품이 가공 대상에서 제외되었는지 - 금지어 포함 시
    private final String processedProductName; // 가공된 상품명
}