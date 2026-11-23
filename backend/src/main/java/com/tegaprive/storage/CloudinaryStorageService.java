package com.tegaprive.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

/**
 * Production storage via Cloudinary. Only active when tp.storage.provider=cloudinary AND
 * credentials are configured (tp.storage.cloudinary-* or CLOUDINARY_URL env). Without
 * credentials it fails fast at construction — it never pretends to work.
 */
@Component
@ConditionalOnProperty(prefix = "tp.storage", name = "provider", havingValue = "cloudinary")
public class CloudinaryStorageService implements StorageService {

  private final Cloudinary cloudinary;

  public CloudinaryStorageService(StorageProperties properties) {
    Map<String, String> config = new HashMap<>();
    if (properties.cloudinaryCloudName() != null) {
      config.put("cloud_name", properties.cloudinaryCloudName());
    }
    if (properties.cloudinaryApiKey() != null) {
      config.put("api_key", properties.cloudinaryApiKey());
    }
    if (properties.cloudinaryApiSecret() != null) {
      config.put("api_secret", properties.cloudinaryApiSecret());
    }
    String cloudName = config.get("cloud_name");
    if (cloudName == null || cloudName.isBlank()) {
      throw new IllegalStateException(
          "Cloudinary storage selected but tp.storage.cloudinary-cloud-name / api-key / api-secret "
              + "(or CLOUDINARY_URL) are not configured.");
    }
    this.cloudinary = new Cloudinary(config);
  }

  @Override
  @SuppressWarnings("unchecked")
  public UploadResult upload(MultipartFile file, String folder) {
    if (file == null || file.isEmpty()) {
      throw new IllegalStateException("File is empty");
    }
    try {
      Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(),
          ObjectUtils.asMap("folder", folder == null ? "" : folder, "resource_type", "auto"));
      return new UploadResult((String) result.get("secure_url"), (String) result.get("public_id"));
    } catch (IOException e) {
      throw new RuntimeException("Cloudinary upload failed", e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void delete(String publicId) {
    if (publicId == null || publicId.isBlank()) {
      return;
    }
    try {
      cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    } catch (IOException e) {
      throw new RuntimeException("Cloudinary delete failed", e);
    }
  }
}
