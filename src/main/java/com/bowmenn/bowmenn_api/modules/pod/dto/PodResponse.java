package com.bowmenn.bowmenn_api.modules.pod.dto;

import com.bowmenn.bowmenn_api.modules.pod.ProofOfDelivery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PodResponse {
    private UUID id;
    private UUID shipmentId;
    private String imageUrl;
    private String imageFileId;
    private String note;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;

    public static PodResponse from(ProofOfDelivery pod) {
        return PodResponse.builder()
            .id(pod.getId())
            .shipmentId(pod.getShipment() != null ? pod.getShipment().getId() : null)
            .imageUrl(pod.getImageUrl())
            .imageFileId(pod.getImageFileId())
            .note(pod.getNote())
            .uploadedBy(pod.getUploadedBy() != null ? pod.getUploadedBy().getId() : null)
            .uploadedAt(pod.getUploadedAt())
            .build();
    }
}
