package com.tegaprive.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.tegaprive.common.exception.BadRequestException;

/**
 * Dev-only storage: saves files to a local directory served at /uploads/** (see WebConfig).
 * Never used in production — production sets tp.storage.provider=cloudinary.
 */
@Component
@ConditionalOnProperty(prefix = "tp.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalDevStorageService implements StorageService {

  private static final Set<String> ALLOWED = Set.of(
      "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf");

  private final Path root;

  public LocalDevStorageService(StorageProperties properties) {
    Path base = Paths.get(properties.localDir() == null || properties.localDir().isBlank()
        ? "uploads" : properties.localDir()).toAbsolutePath().normalize();
    try {
      Files.createDirectories(base);
    } catch (IOException e) {
      throw new IllegalStateException("Could not create local storage directory: " + base, e);
    }
    this.root = base;
  }

  @Override
  public UploadResult upload(MultipartFile file, String folder) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("File is empty");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED.contains(contentType)) {
      throw new BadRequestException("Unsupported file type: " + contentType);
    }
    String ext = extension(file, contentType);
    String publicId = (folder == null || folder.isBlank() ? "" : folder + "/") + UUID.randomUUID() + ext;
    try {
      Path target = root.resolve(publicId).normalize();
      Files.createDirectories(target.getParent());
      file.transferTo(target);
    } catch (IOException e) {
      throw new RuntimeException("Failed to store file", e);
    }
    return new UploadResult("/uploads/" + publicId, publicId);
  }

  @Override
  public void delete(String publicId) {
    if (publicId == null || publicId.isBlank()) {
      return;
    }
    try {
      Files.deleteIfExists(root.resolve(publicId).normalize());
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete file", e);
    }
  }

  private String extension(MultipartFile file, String contentType) {
    String original = file.getOriginalFilename();
    if (original != null && original.contains(".")) {
      return original.substring(original.lastIndexOf('.'));
    }
    return contentType.equals("application/pdf") ? ".pdf" : "";
  }
}
