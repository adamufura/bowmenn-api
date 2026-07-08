package com.bowmenn.bowmenn_api.common.util;

import com.bowmenn.bowmenn_api.modules.shipment.TruckType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PricingUtil {

    private PricingUtil() {
    }

    // Haversine formula to calculate distance between two lat/lng points in KM
    public static double calculateDistanceKm(
            double lat1, double lng1,
            double lat2, double lng2) {
        final int EARTH_RADIUS_KM = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    // Calculate price based on distance and truck type
    public static double calculatePrice(double distanceKm, TruckType truckType) {
        double basePrice = distanceKm * truckType.getPricePerKm();
        double platformCommission = basePrice * 0.10; // 10% Bowmenn commission
        double minimumPrice = 5000.0;
        return Math.max(basePrice + platformCommission, minimumPrice);
    }

    // Generate tracking number: BWN-YYYYMMDD-XXXXX
    public static String generateTrackingNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "BWN-" + date + "-" + random;
    }
}
