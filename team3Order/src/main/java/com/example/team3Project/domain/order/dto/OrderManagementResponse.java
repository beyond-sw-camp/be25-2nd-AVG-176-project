package com.example.team3Project.domain.order.dto;

import lombok.Data;

@Data
public class OrderManagementResponse {

    private Long orderId;
    private Long shipmentId;

    private String autoOrderStatus;
    private String shipmentStatus;
    private String trackingNumber;
    private String courier;

    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String customsNumber;

    private Long dummyCoupangProductId;
    private String productName;
    private int quantity;

    private String overseasMall;

    private int paymentAmount;

    private int margin;
}
