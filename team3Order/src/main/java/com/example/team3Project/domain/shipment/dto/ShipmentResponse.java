package com.example.team3Project.domain.shipment.dto;

import com.example.team3Project.domain.shipment.enums.Shipment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ShipmentResponse {
    private Long id;
    private Long orderId;
    private String trackingNumber;
    private String courier;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ShipmentResponse from(Shipment shipment) {
        return ShipmentResponse.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrderId())
                .trackingNumber(shipment.getTrackingNumber())
                .courier(shipment.getCourier())
                .status(shipment.getStatus().name())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
