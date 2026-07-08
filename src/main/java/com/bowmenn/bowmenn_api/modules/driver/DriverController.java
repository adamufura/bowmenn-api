package com.bowmenn.bowmenn_api.modules.driver;

import com.bowmenn.bowmenn_api.common.exception.ResourceNotFoundException;
import com.bowmenn.bowmenn_api.common.response.ApiResponse;
import com.bowmenn.bowmenn_api.modules.shipment.ShipmentService;
import com.bowmenn.bowmenn_api.modules.shipment.ShipmentStatus;
import com.bowmenn.bowmenn_api.modules.shipment.dto.ShipmentResponse;
import com.bowmenn.bowmenn_api.modules.shipment.dto.UpdateStatusRequest;
import com.bowmenn.bowmenn_api.modules.user.User;
import com.bowmenn.bowmenn_api.modules.user.UserRepository;
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
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@Tag(name = "Driver Portal")
public class DriverController {

    private final ShipmentService shipmentService;
    private final UserRepository userRepository;

    @GetMapping("/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getMyShipments(
            @AuthenticationPrincipal UserDetails userDetails) {
        User driver = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        List<ShipmentResponse> shipments = shipmentService
            .getShipmentsByDriverId(driver.getId());
        return ResponseEntity.ok(ApiResponse.success("Shipments retrieved", shipments));
    }

    @PutMapping("/shipments/{id}/accept")
    public ResponseEntity<ApiResponse<ShipmentResponse>> acceptShipment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(ShipmentStatus.ACCEPTED);
        req.setNote("Accepted by driver");
        return ResponseEntity.ok(ApiResponse.success("Shipment accepted",
            shipmentService.updateStatus(id, req, userDetails.getUsername())));
    }

    @PutMapping("/shipments/{id}/reject")
    public ResponseEntity<ApiResponse<ShipmentResponse>> rejectShipment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(ShipmentStatus.REJECTED);
        req.setNote("Rejected by driver");
        return ResponseEntity.ok(ApiResponse.success("Shipment rejected",
            shipmentService.updateStatus(id, req, userDetails.getUsername())));
    }

    @PutMapping("/shipments/{id}/status")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Status updated",
            shipmentService.updateStatus(id, request, userDetails.getUsername())));
    }
}
