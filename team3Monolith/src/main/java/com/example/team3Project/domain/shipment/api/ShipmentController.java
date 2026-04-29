package com.example.team3Project.domain.shipment.api;

import com.example.team3Project.domain.shipment.application.ShipmentService;
import com.example.team3Project.domain.shipment.dto.ShipmentCreateRequest;
import com.example.team3Project.domain.shipment.dto.ShipmentResponse;
import com.example.team3Project.domain.shipment.dto.ShipmentStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService shipmentService;

    @PostMapping("/create")
    public ResponseEntity<ShipmentResponse> createShipment(@RequestBody ShipmentCreateRequest request) {
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orderId")
    public ResponseEntity<ShipmentResponse> getShipmentByOrderId(@RequestParam Long orderId) {
        ShipmentResponse response = shipmentService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shipmentId}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable Long shipmentId,
            @RequestBody ShipmentStatusUpdateRequest request
    ) {
        ShipmentResponse response = shipmentService.updateShipmentStatus(shipmentId, request);
        return ResponseEntity.ok(response);
    }
}
