package com.example.team3Project.domain.shipment.application;

import com.example.team3Project.domain.order.dao.Order;
import com.example.team3Project.domain.order.dao.OrderRepository;
import com.example.team3Project.domain.shipment.dao.ShipmentRepository;
import com.example.team3Project.domain.shipment.dto.ShipmentCreateRequest;
import com.example.team3Project.domain.shipment.dto.ShipmentResponse;
import com.example.team3Project.domain.shipment.dto.ShipmentStatusUpdateRequest;
import com.example.team3Project.domain.shipment.enums.Shipment;
import com.example.team3Project.domain.shipment.enums.ShipmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private static final String DEFAULT_COURIER = "CJ대한통운";

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;

    public ShipmentResponse createShipment(ShipmentCreateRequest request) {
        validateCreateRequest(request);

        if (shipmentRepository.existsByOrder_Id(request.getOrderId())) {
            throw new IllegalStateException("해당 주문의 배송 정보가 이미 존재합니다.");
        }

        if (shipmentRepository.existsByTrackingNumber(request.getTrackingNumber())) {
            throw new IllegalStateException("이미 등록된 송장 번호입니다.");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다."));

        Shipment shipment = Shipment.builder()
                .order(order)
                .trackingNumber(request.getTrackingNumber())
                .courier(request.getCourier())
                .status(ShipmentStatus.READY)
                .build();

        return ShipmentResponse.from(shipmentRepository.save(shipment));
    }

    public ShipmentResponse createDefaultShipment(Long orderId) {
        return shipmentRepository.findByOrder_Id(orderId)
                .map(ShipmentResponse::from)
                .orElseGet(() -> createShipment(new ShipmentCreateRequest(
                        orderId,
                        generateTrackingNumber(),
                        DEFAULT_COURIER
                )));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문의 배송 정보가 존재하지 않습니다."));

        return ShipmentResponse.from(shipment);
    }

    public ShipmentResponse updateShipmentStatus(Long shipmentId, ShipmentStatusUpdateRequest request) {
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 배송 정보가 존재하지 않습니다."));

        ShipmentStatus status;
        try {
            status = ShipmentStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status는 READY, SHIPPING, DELIVERED 중 하나여야 합니다.");
        }

        shipment.updateStatus(status);
        return ShipmentResponse.from(shipment);
    }

    private void validateCreateRequest(ShipmentCreateRequest request) {
        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("orderId는 필수입니다.");
        }
        if (request.getTrackingNumber() == null || request.getTrackingNumber().isBlank()) {
            throw new IllegalArgumentException("trackingNumber는 필수입니다.");
        }
        if (request.getCourier() == null || request.getCourier().isBlank()) {
            throw new IllegalArgumentException("courier는 필수입니다.");
        }
    }

    private String generateTrackingNumber() {
        while (true) {
            int length = ThreadLocalRandom.current().nextInt(10, 13);
            StringBuilder builder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                builder.append(ThreadLocalRandom.current().nextInt(10));
            }

            String trackingNumber = builder.toString();
            if (!shipmentRepository.existsByTrackingNumber(trackingNumber)) {
                return trackingNumber;
            }
        }
    }
}
