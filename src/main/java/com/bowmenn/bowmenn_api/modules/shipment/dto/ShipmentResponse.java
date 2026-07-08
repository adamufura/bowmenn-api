package com.bowmenn.bowmenn_api.modules.shipment.dto;

import com.bowmenn.bowmenn_api.modules.pod.dto.PodResponse;
import com.bowmenn.bowmenn_api.modules.shipment.Shipment;
import com.bowmenn.bowmenn_api.modules.shipment.ShipmentStatus;
import com.bowmenn.bowmenn_api.modules.shipment.TruckType;
import com.bowmenn.bowmenn_api.modules.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentResponse {
    private UUID id;
    private String trackingNumber;
    private UserResponse customer;
    private UserResponse driver;
    private String pickupAddress;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private String deliveryAddress;
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;
    private String cargoDescription;
    private BigDecimal cargoWeight;
    private TruckType truckType;
    private ShipmentStatus status;
    private BigDecimal estimatedDistanceKm;
    private BigDecimal estimatedPrice;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PodResponse pod;

    public static ShipmentResponse from(Shipment s) {
        return ShipmentResponse.builder()
            .id(s.getId())
            .trackingNumber(s.getTrackingNumber())
            .customer(s.getCustomer() != null ? UserResponse.from(s.getCustomer()) : null)
            .driver(s.getDriver() != null ? UserResponse.from(s.getDriver()) : null)
            .pickupAddress(s.getPickupAddress())
            .pickupLat(s.getPickupLat())
            .pickupLng(s.getPickupLng())
            .deliveryAddress(s.getDeliveryAddress())
            .deliveryLat(s.getDeliveryLat())
            .deliveryLng(s.getDeliveryLng())
            .cargoDescription(s.getCargoDescription())
            .cargoWeight(s.getCargoWeight())
            .truckType(s.getTruckType())
            .status(s.getStatus())
            .estimatedDistanceKm(s.getEstimatedDistanceKm())
            .estimatedPrice(s.getEstimatedPrice())
            .notes(s.getNotes())
            .createdAt(s.getCreatedAt())
            .updatedAt(s.getUpdatedAt())
            .build();
    }
}
