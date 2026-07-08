package com.bowmenn.bowmenn_api.modules.pod;

import com.bowmenn.bowmenn_api.modules.shipment.Shipment;
import com.bowmenn.bowmenn_api.modules.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "proof_of_delivery")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProofOfDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_file_id")
    private String imageFileId;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
