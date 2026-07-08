package com.bowmenn.bowmenn_api.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over blob storage so the storage backend (ImageKit, S3, local disk, …)
 * can be swapped without touching the domain modules.
 */
public interface FileStorageService {

    /**
     * Uploads the given file and returns its public location.
     *
     * @param file the uploaded multipart file (must be non-empty)
     * @return the stored file's URL and provider metadata
     */
    StoredFile upload(MultipartFile file);
}
