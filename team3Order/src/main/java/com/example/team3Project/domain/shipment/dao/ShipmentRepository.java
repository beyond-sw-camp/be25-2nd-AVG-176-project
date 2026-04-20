package com.example.team3Project.domain.shipment.dao;

import com.example.team3Project.domain.shipment.enums.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrder_Id(Long orderId);

    boolean existsByOrder_Id(Long orderId);

    boolean existsByTrackingNumber(String trackingNumber);
}
