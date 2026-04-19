package com.example.team3Project.domain.order.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private Long userId;

    // 주문 상품
    private Long dummyCoupangProductId;
    private Long dummyCoupangProductOptionId;
    private int quantity;

    // 고객 정보
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String customsNumber;

    private List<OrderItemRequest> items; // 레거시 호환용 (사용 안 함)
    private Long cardId;
}
