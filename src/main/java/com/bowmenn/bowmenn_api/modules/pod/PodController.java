package com.bowmenn.bowmenn_api.modules.pod;

import com.bowmenn.bowmenn_api.common.response.ApiResponse;
import com.bowmenn.bowmenn_api.modules.pod.dto.PodResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/pod")
@RequiredArgsConstructor
@Tag(name = "Proof of Delivery")
public class PodController {

    private final PodService podService;

    @PostMapping(value = "/{shipmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PodResponse>> uploadPod(
            @PathVariable UUID shipmentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("POD uploaded",
            podService.uploadPod(shipmentId, file, note, userDetails.getUsername())));
    }

    @GetMapping("/{shipmentId}")
    public ResponseEntity<ApiResponse<PodResponse>> getPod(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("POD retrieved",
            podService.getPodByShipmentId(shipmentId, userDetails.getUsername())));
    }
}
