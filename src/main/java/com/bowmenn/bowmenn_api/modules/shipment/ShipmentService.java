package com.bowmenn.bowmenn_api.modules.shipment;

import com.bowmenn.bowmenn_api.common.exception.BadRequestException;
import com.bowmenn.bowmenn_api.common.exception.ResourceNotFoundException;
import com.bowmenn.bowmenn_api.common.exception.UnauthorizedException;
import com.bowmenn.bowmenn_api.common.util.PricingUtil;
import com.bowmenn.bowmenn_api.modules.shipment.dto.CreateShipmentRequest;
import com.bowmenn.bowmenn_api.modules.shipment.dto.ShipmentResponse;
import com.bowmenn.bowmenn_api.modules.shipment.dto.UpdateStatusRequest;
import com.bowmenn.bowmenn_api.modules.user.User;
import com.bowmenn.bowmenn_api.modules.user.UserRepository;
import com.bowmenn.bowmenn_api.modules.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final ShipmentStatusLogRepository statusLogRepository;

    public ShipmentResponse createShipment(CreateShipmentRequest request, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        double distanceKm = 0;
        double price = 5000; // minimum price

        if (request.getPickupLat() != null && request.getPickupLng() != null
                && request.getDeliveryLat() != null && request.getDeliveryLng() != null) {
            distanceKm = PricingUtil.calculateDistanceKm(
                request.getPickupLat().doubleValue(),
                request.getPickupLng().doubleValue(),
                request.getDeliveryLat().doubleValue(),
                request.getDeliveryLng().doubleValue());
            price = PricingUtil.calculatePrice(distanceKm, request.getTruckType());
        }

        Shipment shipment = Shipment.builder()
            .trackingNumber(PricingUtil.generateTrackingNumber())
            .customer(customer)
            .pickupAddress(request.getPickupAddress())
            .pickupLat(request.getPickupLat())
            .pickupLng(request.getPickupLng())
            .deliveryAddress(request.getDeliveryAddress())
            .deliveryLat(request.getDeliveryLat())
            .deliveryLng(request.getDeliveryLng())
            .cargoDescription(request.getCargoDescription())
            .cargoWeight(request.getCargoWeight())
            .truckType(request.getTruckType())
            .status(ShipmentStatus.PENDING)
            .estimatedDistanceKm(BigDecimal.valueOf(distanceKm))
            .estimatedPrice(BigDecimal.valueOf(price))
            .notes(request.getNotes())
            .build();

        shipment = shipmentRepository.save(shipment);
        logStatusChange(shipment, null, ShipmentStatus.PENDING, customer, "Shipment created");

        return ShipmentResponse.from(shipment);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getMyShipments(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return shipmentRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
            .stream().map(ShipmentResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsByDriverId(UUID driverId) {
        return shipmentRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
            .stream().map(ShipmentResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(UUID id, String requesterEmail) {
        Shipment shipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        assertCanView(shipment, requireUser(requesterEmail));
        return ShipmentResponse.from(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTracking(String trackingNumber, String requesterEmail) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        assertCanView(shipment, requireUser(requesterEmail));
        return ShipmentResponse.from(shipment);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * A shipment is visible to an admin, to the customer who booked it, and to the
     * driver it is assigned to. Everyone else is denied.
     */
    public static void assertCanView(Shipment shipment, User user) {
        if (user.getRole() == UserRole.ADMIN) return;
        if (user.getRole() == UserRole.CUSTOMER
                && shipment.getCustomer() != null
                && shipment.getCustomer().getId().equals(user.getId())) return;
        if (user.getRole() == UserRole.DRIVER
                && shipment.getDriver() != null
                && shipment.getDriver().getId().equals(user.getId())) return;
        throw new UnauthorizedException("You do not have access to this shipment");
    }

    /**
     * Only an admin, or the driver the shipment is assigned to, may move it through
     * the delivery lifecycle.
     */
    public static void assertCanUpdate(Shipment shipment, User user) {
        if (user.getRole() == UserRole.ADMIN) return;
        if (user.getRole() == UserRole.DRIVER
                && shipment.getDriver() != null
                && shipment.getDriver().getId().equals(user.getId())) return;
        throw new UnauthorizedException("You are not assigned to this shipment");
    }

    // Admin operations
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(ShipmentResponse::from).collect(Collectors.toList());
    }

    public ShipmentResponse assignDriver(UUID shipmentId, UUID driverId, String adminEmail) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        User driver = userRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new BadRequestException("User is not a driver");
        }

        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setDriver(driver);
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        shipment = shipmentRepository.save(shipment);
        logStatusChange(shipment, oldStatus, ShipmentStatus.ASSIGNED, admin, "Driver assigned by admin");

        return ShipmentResponse.from(shipment);
    }

    public ShipmentResponse updateStatus(UUID shipmentId, UpdateStatusRequest request,
            String userEmail) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        User user = requireUser(userEmail);
        assertCanUpdate(shipment, user);

        ShipmentStatus currentStatus = shipment.getStatus();
        ShipmentStatus newStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BadRequestException(
                "Cannot transition from " + currentStatus + " to " + newStatus);
        }

        shipment.setStatus(newStatus);
        shipment = shipmentRepository.save(shipment);
        logStatusChange(shipment, currentStatus, newStatus, user, request.getNote());

        return ShipmentResponse.from(shipment);
    }

    private void logStatusChange(Shipment shipment, ShipmentStatus oldStatus,
            ShipmentStatus newStatus, User changedBy, String note) {
        ShipmentStatusLog log = ShipmentStatusLog.builder()
            .shipment(shipment)
            .oldStatus(oldStatus != null ? oldStatus.name() : null)
            .newStatus(newStatus.name())
            .changedBy(changedBy)
            .note(note)
            .build();
        statusLogRepository.save(log);
    }
}
