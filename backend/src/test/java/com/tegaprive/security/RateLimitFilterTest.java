package com.tegaprive.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

/**
 * Unit test for rate limiting logic — no Spring context needed.
 * Uses Bucket4j 8.x builder API.
 */
class RateLimitFilterTest {

  @Test
  void allowsConfiguredCapacityThenBlocks() {
    Bucket bucket = Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(3)
            .refillIntervally(3, Duration.ofMinutes(1)).build())
        .build();

    assertThat(bucket.tryConsume(1)).isTrue();
    assertThat(bucket.tryConsume(1)).isTrue();
    assertThat(bucket.tryConsume(1)).isTrue();
    // 4th must be blocked
    assertThat(bucket.tryConsume(1)).isFalse();
  }

  @Test
  void availableTokensAfterPartialConsumption() {
    Bucket bucket = Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(5)
            .refillIntervally(5, Duration.ofMinutes(1)).build())
        .build();

    bucket.tryConsume(2);
    assertThat(bucket.getAvailableTokens()).isEqualTo(3);
  }
}
