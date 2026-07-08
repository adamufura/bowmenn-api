package com.bowmenn.bowmenn_api.modules.shipment.dto;

import com.bowmenn.bowmenn_api.modules.shipment.TruckType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateShipmentRequest {
    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    private BigDecimal pickupLat;
    private BigDecimal pickupLng;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;

    @NotBlank(message = "Cargo description is required")
    private String cargoDescription;

    @NotNull(message = "Cargo weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be greater than 0")
    private BigDecimal cargoWeight;

    @NotNull(message = "Truck type is required")
    private TruckType truckType;

    private String notes;
}
