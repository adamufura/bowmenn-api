package com.bowmenn.bowmenn_api.modules.pod;

import com.bowmenn.bowmenn_api.common.exception.BadRequestException;
import com.bowmenn.bowmenn_api.common.exception.ResourceNotFoundException;
import com.bowmenn.bowmenn_api.common.storage.FileStorageService;
import com.bowmenn.bowmenn_api.common.storage.StoredFile;
import com.bowmenn.bowmenn_api.modules.pod.dto.PodResponse;
import com.bowmenn.bowmenn_api.modules.shipment.Shipment;
import com.bowmenn.bowmenn_api.modules.shipment.ShipmentRepository;
import com.bowmenn.bowmenn_api.modules.shipment.ShipmentStatus;
import com.bowmenn.bowmenn_api.modules.user.User;
import com.bowmenn.bowmenn_api.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PodService {

    private final ProofOfDeliveryRepository podRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public PodResponse uploadPod(UUID shipmentId, MultipartFile file,
            String note, String uploaderEmail) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        User uploader = userRepository.findByEmail(uploaderEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (podRepository.existsByShipmentId(shipmentId)) {
            throw new BadRequestException("Proof of delivery already exists for this shipment");
        }

        // Upload to the configured storage provider (ImageKit) and keep the returned URL.
        StoredFile stored = fileStorageService.upload(file);

        ProofOfDelivery pod = ProofOfDelivery.builder()
            .shipment(shipment)
            .imageUrl(stored.url())
            .imageFileId(stored.fileId())
            .note(note)
            .uploadedBy(uploader)
            .build();

        pod = podRepository.save(pod);

        // Update shipment status to DELIVERED if in transit
        if (shipment.getStatus() == ShipmentStatus.IN_TRANSIT) {
            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipmentRepository.save(shipment);
        }

        return PodResponse.from(pod);
    }

    @Transactional(readOnly = true)
    public PodResponse getPodByShipmentId(UUID shipmentId) {
        ProofOfDelivery pod = podRepository.findByShipmentId(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Proof of delivery not found"));
        return PodResponse.from(pod);
    }
}
