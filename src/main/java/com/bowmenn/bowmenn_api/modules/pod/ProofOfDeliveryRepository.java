package com.bowmenn.bowmenn_api.modules.pod;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, UUID> {
    Optional<ProofOfDelivery> findByShipmentId(UUID shipmentId);
    boolean existsByShipmentId(UUID shipmentId);
}
