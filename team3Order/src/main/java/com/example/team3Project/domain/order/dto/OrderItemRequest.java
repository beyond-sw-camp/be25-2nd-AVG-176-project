package com.example.team3Project.domain.order.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long productId;
    private String productName;
    private int price;
    private int quantity;
}
