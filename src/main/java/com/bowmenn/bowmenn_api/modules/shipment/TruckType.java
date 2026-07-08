package com.bowmenn.bowmenn_api.modules.shipment;

public enum TruckType {
    MINI(150.0, 1000.0),
    MEDIUM(250.0, 5000.0),
    LARGE(400.0, 15000.0);

    private final double pricePerKm;
    private final double capacityKg;

    TruckType(double pricePerKm, double capacityKg) {
        this.pricePerKm = pricePerKm;
        this.capacityKg = capacityKg;
    }

    public double getPricePerKm() { return pricePerKm; }
    public double getCapacityKg() { return capacityKg; }
}
