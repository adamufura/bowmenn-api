package com.bowmenn.bowmenn_api.modules.shipment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignDriverRequest {
    @NotNull(message = "Driver ID is required")
    private UUID driverId;
}
