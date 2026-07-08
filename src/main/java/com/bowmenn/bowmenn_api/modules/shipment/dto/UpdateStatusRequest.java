package com.bowmenn.bowmenn_api.modules.shipment.dto;

import com.bowmenn.bowmenn_api.modules.shipment.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private ShipmentStatus status;
    private String note;
}
