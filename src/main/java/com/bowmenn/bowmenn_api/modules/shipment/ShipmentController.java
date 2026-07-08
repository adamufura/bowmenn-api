package com.bowmenn.bowmenn_api.modules.shipment;

import com.bowmenn.bowmenn_api.common.response.ApiResponse;
import com.bowmenn.bowmenn_api.modules.shipment.dto.CreateShipmentRequest;
import com.bowmenn.bowmenn_api.modules.shipment.dto.ShipmentResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments - Customer")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @Valid @RequestBody CreateShipmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Shipment created",
            shipmentService.createShipment(request, userDetails.getUsername())));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getMyShipments(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Shipments retrieved",
            shipmentService.getMyShipments(userDetails.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Shipment retrieved",
            shipmentService.getShipmentById(id)));
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> trackShipment(
            @PathVariable String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.success("Shipment retrieved",
            shipmentService.getShipmentByTracking(trackingNumber)));
    }
}
