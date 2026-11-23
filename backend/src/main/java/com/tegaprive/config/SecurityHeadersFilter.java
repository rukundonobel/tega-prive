package com.tegaprive.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds security headers to every HTTP response.
 * These headers are defence-in-depth and complement the JWT auth and CORS config.
 *
 * Content-Security-Policy is intentionally permissive for the API (JSON only)
 * — the React SPA is served separately and sets its own CSP via Nginx.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    // Prevent MIME-type sniffing
    response.setHeader("X-Content-Type-Options", "nosniff");

    // Prevent clickjacking — API responses should never be framed
    response.setHeader("X-Frame-Options", "DENY");

    // XSS protection (legacy browser support)
    response.setHeader("X-XSS-Protection", "1; mode=block");

    // Referrer policy
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

    // Permissions policy — no camera/mic/geolocation on the API
    response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

    // HSTS — enforce HTTPS for 1 year (enable only when TLS is confirmed)
    // response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

    chain.doFilter(request, response);
  }
}
