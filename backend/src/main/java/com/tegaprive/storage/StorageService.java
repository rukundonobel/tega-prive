package com.tegaprive.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over media/document storage so the marketplace never depends on a single provider.
 * Implementations: LocalDevStorageService (dev), CloudinaryStorageService (production).
 */
public interface StorageService {

  /** Stores the uploaded file under the given folder and returns its public URL + provider id. */
  UploadResult upload(MultipartFile file, String folder);

  /** Removes a previously uploaded asset by its provider public id. */
  void delete(String publicId);

  record UploadResult(String url, String publicId) {
  }
}
