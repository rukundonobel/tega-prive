package com.tegaprive.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT configuration bound from tp.jwt.* properties. */
@ConfigurationProperties(prefix = "tp.jwt")
public record JwtProperties(String secret, long accessExpirationMin, long refreshExpirationDays) {
}
