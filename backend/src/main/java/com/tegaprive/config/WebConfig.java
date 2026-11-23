package com.tegaprive.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tegaprive.storage.StorageProperties;

/**
 * Serves locally-stored uploads at /uploads/** (dev only, when tp.storage.provider=local).
 * In production the URLs point to Cloudinary and this handler is unused.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final StorageProperties storageProperties;

  public WebConfig(StorageProperties storageProperties) {
    this.storageProperties = storageProperties;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String dir = storageProperties.localDir() == null || storageProperties.localDir().isBlank()
        ? "uploads" : storageProperties.localDir();
    String location = "file:" + Paths.get(dir).toAbsolutePath().normalize() + "/";
    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}
