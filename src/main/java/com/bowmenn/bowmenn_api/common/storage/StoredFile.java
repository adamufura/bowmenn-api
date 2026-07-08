package com.bowmenn.bowmenn_api.common.storage;

/**
 * Result of storing a file with a {@link FileStorageService}.
 *
 * @param url          publicly accessible URL of the stored file
 * @param fileId       provider-specific identifier (used for later deletion/updates)
 * @param thumbnailUrl provider-generated thumbnail URL, if any (may be null)
 */
public record StoredFile(String url, String fileId, String thumbnailUrl) {
}
