package com.tegaprive.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Storage configuration bound from tp.storage.*.
 * provider: "local" (dev, default) or "cloudinary" (production).
 */
@ConfigurationProperties(prefix = "tp.storage")
public record StorageProperties(
    String provider,
    String localDir,
    String cloudinaryCloudName,
    String cloudinaryApiKey,
    String cloudinaryApiSecret) {
}
