package com.tegaprive.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** CORS allowed origins bound from tp.cors.allowed-origins (comma-separated). */
@ConfigurationProperties(prefix = "tp.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
