package com.tegaprive.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Token-bucket rate limiter for sensitive endpoints (Bucket4j 8.x).
 *
 * Rules (per IP per endpoint group):
 *   /api/auth/login    — 10 requests / minute
 *   /api/auth/register — 5 requests / minute
 *   /api/payments/**   — 20 requests / minute
 *   file-upload paths  — 10 requests / minute
 *
 * In-process ConcurrentHashMap — replace with bucket4j-redis for multi-instance.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    String path = request.getRequestURI();
    String group = rateGroup(path);

    if (group == null) {
      chain.doFilter(request, response);
      return;
    }

    String ip = resolveIp(request);
    Bucket bucket = buckets.computeIfAbsent(ip + "::" + group, k -> buildBucket(group));

    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
    } else {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write(
          "{\"success\":false,\"message\":\"Too many requests. Please try again shortly.\"}");
    }
  }

  private String rateGroup(String path) {
    if (path.startsWith("/api/auth/login")) return "auth-login";
    if (path.startsWith("/api/auth/register")) return "auth-register";
    if (path.startsWith("/api/payments")) return "payments";
    if (path.contains("/photo") || path.contains("/photos") || path.contains("/documents"))
      return "file-upload";
    return null;
  }

  private Bucket buildBucket(String group) {
    // Bucket4j 8.x uses Bandwidth.builder() fluent API
    Bandwidth limit = switch (group) {
      case "auth-login" ->
          Bandwidth.builder().capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build();
      case "auth-register" ->
          Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build();
      case "payments" ->
          Bandwidth.builder().capacity(20).refillIntervally(20, Duration.ofMinutes(1)).build();
      default ->    // file-upload
          Bandwidth.builder().capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build();
    };
    return Bucket.builder().addLimit(limit).build();
  }

  private String resolveIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
