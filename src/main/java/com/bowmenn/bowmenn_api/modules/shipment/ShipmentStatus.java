package com.bowmenn.bowmenn_api.modules.shipment;

public enum ShipmentStatus {
    PENDING, ASSIGNED, ACCEPTED, PICKED_UP, IN_TRANSIT, DELIVERED, CANCELLED, REJECTED;

    public boolean canTransitionTo(ShipmentStatus next) {
        return switch (this) {
            case PENDING -> next == ASSIGNED || next == CANCELLED;
            case ASSIGNED -> next == ACCEPTED || next == REJECTED || next == CANCELLED;
            case ACCEPTED -> next == PICKED_UP || next == CANCELLED;
            case PICKED_UP -> next == IN_TRANSIT;
            case IN_TRANSIT -> next == DELIVERED;
            default -> false;
        };
    }
}
