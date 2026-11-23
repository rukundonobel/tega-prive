package com.tegaprive.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.tegaprive.common.exception.BadRequestException;

/** Unit test for the local dev storage provider (no Spring context). */
class LocalDevStorageServiceTest {

  @TempDir
  private Path tempDir;

  private LocalDevStorageService service;

  @BeforeEach
  void setUp() {
    service = new LocalDevStorageService(
        new StorageProperties("local", tempDir.toString(), null, null, null));
  }

  @Test
  void storesFileAndReturnsUrl() {
    MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg",
        new byte[] {1, 2, 3, 4});

    StorageService.UploadResult result = service.upload(file, "vehicles");

    assertThat(result.url()).startsWith("/uploads/vehicles/");
    assertThat(result.publicId()).startsWith("vehicles/");
    assertThat(Files.exists(tempDir.resolve(result.publicId()))).isTrue();
  }

  @Test
  void rejectsUnsupportedType() {
    MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", "hi".getBytes());
    assertThatThrownBy(() -> service.upload(file, "vehicles")).isInstanceOf(BadRequestException.class);
  }

  @Test
  void rejectsEmptyFile() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[] {});
    assertThatThrownBy(() -> service.upload(file, "vehicles")).isInstanceOf(BadRequestException.class);
  }
}
