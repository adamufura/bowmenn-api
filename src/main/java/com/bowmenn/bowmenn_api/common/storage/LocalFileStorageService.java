package com.bowmenn.bowmenn_api.common.storage;

import com.bowmenn.bowmenn_api.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Default storage backend: writes files to a local directory and serves them
 * from {@code /uploads/**}. Requires no third-party credentials, so the app
 * runs out of the box after a clone.
 *
 * <p>Not suitable for multi-instance deployments (the disk is not shared) — set
 * {@code storage.provider=imagekit} in production.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final String uploadDirectory;

    public LocalFileStorageService(@Value("${storage.local.directory:uploads}") String uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
        log.info("Using LOCAL file storage (directory: {})", uploadDirectory);
    }

    @Override
    public StoredFile upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        String original = StringUtils.hasText(file.getOriginalFilename())
            ? Paths.get(file.getOriginalFilename()).getFileName().toString()
            : "upload";
        String fileName = UUID.randomUUID() + "_" + original;

        Path uploadPath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            Path target = uploadPath.resolve(fileName).normalize();
            // Guard against path traversal via a crafted filename.
            if (!target.startsWith(uploadPath)) {
                throw new BadRequestException("Invalid file name");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Local file upload failed", e);
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }

        return new StoredFile("/uploads/" + fileName, fileName, null);
    }
}
