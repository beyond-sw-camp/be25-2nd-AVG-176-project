package com.example.team3Project.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderCreateResponse {
    private Long orderId;
    private String orderStatus;
    private String autoOrderStatus;
    private String paymentStatus;
    private boolean success;
    private String message;
}
