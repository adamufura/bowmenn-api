package com.bowmenn.bowmenn_api.modules.admin;

import com.bowmenn.bowmenn_api.common.exception.ResourceNotFoundException;
import com.bowmenn.bowmenn_api.common.response.ApiResponse;
import com.bowmenn.bowmenn_api.modules.shipment.ShipmentService;
import com.bowmenn.bowmenn_api.modules.shipment.ShipmentStatus;
import com.bowmenn.bowmenn_api.modules.shipment.dto.AssignDriverRequest;
import com.bowmenn.bowmenn_api.modules.shipment.dto.ShipmentResponse;
import com.bowmenn.bowmenn_api.modules.shipment.dto.UpdateStatusRequest;
import com.bowmenn.bowmenn_api.modules.user.User;
import com.bowmenn.bowmenn_api.modules.user.UserRepository;
import com.bowmenn.bowmenn_api.modules.user.UserRole;
import com.bowmenn.bowmenn_api.modules.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard")
public class AdminController {

    private final ShipmentService shipmentService;
    private final UserRepository userRepository;

    @GetMapping("/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getAllShipments() {
        return ResponseEntity.ok(ApiResponse.success("All shipments",
            shipmentService.getAllShipments()));
    }

    @PutMapping("/shipments/{id}/assign")
    public ResponseEntity<ApiResponse<ShipmentResponse>> assignDriver(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDriverRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Driver assigned",
            shipmentService.assignDriver(id, request.getDriverId(),
                userDetails.getUsername())));
    }

    @PutMapping("/shipments/{id}/status")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Status updated",
            shipmentService.updateStatus(id, request, userDetails.getUsername())));
    }

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllDrivers() {
        List<UserResponse> drivers = userRepository.findByRole(UserRole.DRIVER)
            .stream().map(UserResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Drivers retrieved", drivers));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllCustomers() {
        List<UserResponse> customers = userRepository.findByRole(UserRole.CUSTOMER)
            .stream().map(UserResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved", customers));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(!user.getIsActive());
        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User status updated",
            UserResponse.from(user)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        List<ShipmentResponse> allShipments = shipmentService.getAllShipments();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalShipments", allShipments.size());
        stats.put("totalDrivers", userRepository.findByRole(UserRole.DRIVER).size());
        stats.put("totalCustomers", userRepository.findByRole(UserRole.CUSTOMER).size());
        stats.put("pendingShipments",
            allShipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.PENDING).count());
        stats.put("completedShipments",
            allShipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).count());
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", stats));
    }
}
