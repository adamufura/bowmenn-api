package com.bowmenn.bowmenn_api.common.storage;

import com.bowmenn.bowmenn_api.common.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * {@link FileStorageService} backed by ImageKit's server-side upload API (V1),
 * authenticated with the account's private key via HTTP Basic auth.
 */
@Service
@Slf4j
public class ImageKitStorageService implements FileStorageService {

    private final RestTemplate restTemplate;
    private final String uploadUrl;
    private final String privateKey;
    private final String folder;

    public ImageKitStorageService(
            @Value("${imagekit.upload-url}") String uploadUrl,
            @Value("${imagekit.private-key}") String privateKey,
            @Value("${imagekit.folder}") String folder) {
        this.uploadUrl = uploadUrl;
        this.privateKey = privateKey;
        this.folder = folder;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public StoredFile upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file: " + e.getMessage());
        }

        String original = StringUtils.hasText(file.getOriginalFilename())
            ? file.getOriginalFilename() : "upload";
        String fileName = UUID.randomUUID() + "_" + original;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // ImageKit server-side auth: Basic base64(privateKey + ":")
        headers.setBasicAuth(privateKey, "");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        body.add("fileName", fileName);
        body.add("folder", folder);
        body.add("useUniqueFileName", "true");

        try {
            ResponseEntity<ImageKitUploadResponse> resp = restTemplate.postForEntity(
                uploadUrl, new HttpEntity<>(body, headers), ImageKitUploadResponse.class);
            ImageKitUploadResponse b = resp.getBody();
            if (b == null || !StringUtils.hasText(b.url)) {
                throw new BadRequestException("Image upload failed: empty response from storage provider");
            }
            return new StoredFile(b.url, b.fileId, b.thumbnailUrl);
        } catch (RestClientException e) {
            log.error("ImageKit upload failed", e);
            throw new BadRequestException("Image upload failed: " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ImageKitUploadResponse {
        public String fileId;
        public String name;
        public String url;
        public String thumbnailUrl;
        public String filePath;
    }
}
