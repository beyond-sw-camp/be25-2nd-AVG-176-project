package com.example.team3Project.domain.shipment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCreateRequest {
    private Long orderId;
    private String trackingNumber;
    private String courier;
}
