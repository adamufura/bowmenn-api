package com.bowmenn.bowmenn_api.modules.shipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<Shipment> findByDriverIdOrderByCreatedAtDesc(UUID driverId);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByStatusOrderByCreatedAtDesc(ShipmentStatus status);
    List<Shipment> findAllByOrderByCreatedAtDesc();
    List<Shipment> findByDriverIdAndStatusIn(UUID driverId, List<ShipmentStatus> statuses);
}
