package com.tegaprive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bootstrap super-admin credentials bound from tp.default-admin.* (optional). */
@ConfigurationProperties(prefix = "tp.default-admin")
public record AdminProperties(String email, String password) {
}
