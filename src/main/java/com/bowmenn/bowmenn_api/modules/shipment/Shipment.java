package com.bowmenn.bowmenn_api.modules.shipment;

import com.bowmenn.bowmenn_api.modules.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tracking_number", unique = true, nullable = false)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_lat")
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng")
    private BigDecimal pickupLng;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_lat")
    private BigDecimal deliveryLat;

    @Column(name = "delivery_lng")
    private BigDecimal deliveryLng;

    @Column(name = "cargo_description", nullable = false)
    private String cargoDescription;

    @Column(name = "cargo_weight", nullable = false)
    private BigDecimal cargoWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "truck_type", nullable = false)
    private TruckType truckType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "estimated_distance_km")
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_price")
    private BigDecimal estimatedPrice;

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
