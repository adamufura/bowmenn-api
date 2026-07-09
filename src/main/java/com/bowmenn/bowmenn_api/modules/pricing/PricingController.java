package com.bowmenn.bowmenn_api.modules.pricing;

import com.bowmenn.bowmenn_api.common.response.ApiResponse;
import com.bowmenn.bowmenn_api.common.util.PricingUtil;
import com.bowmenn.bowmenn_api.modules.shipment.TruckType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public price estimation. Given pickup/delivery coordinates and a truck type,
 * returns the haversine distance and the computed price — the same calculation
 * used when a shipment is created.
 */
@RestController
@RequestMapping("/api/pricing")
@Tag(name = "Pricing")
public class PricingController {

    @GetMapping("/estimate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estimate(
            @RequestParam double pickupLat,
            @RequestParam double pickupLng,
            @RequestParam double deliveryLat,
            @RequestParam double deliveryLng,
            @RequestParam TruckType truckType) {

        double km = PricingUtil.calculateDistanceKm(pickupLat, pickupLng, deliveryLat, deliveryLng);
        double price = PricingUtil.calculatePrice(km, truckType);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("estimatedDistanceKm", BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP));
        data.put("estimatedPrice", BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP));
        data.put("truckType", truckType);

        return ResponseEntity.ok(ApiResponse.success("Price estimated", data));
    }
}
