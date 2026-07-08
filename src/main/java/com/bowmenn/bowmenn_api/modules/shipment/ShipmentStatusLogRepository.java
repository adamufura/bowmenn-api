package com.bowmenn.bowmenn_api.modules.shipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentStatusLogRepository extends JpaRepository<ShipmentStatusLog, UUID> {
    List<ShipmentStatusLog> findByShipmentIdOrderByCreatedAtAsc(UUID shipmentId);
}
