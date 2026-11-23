package com.tegaprive.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Extracts the Bearer access token, validates it, and sets the Spring Security context. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;

  public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String token = resolveToken(request);
    if (token != null) {
      try {
        Jws<Claims> jws = tokenProvider.parse(token);
        if ("access".equals(tokenProvider.getTokenType(jws))) {
          UUID userId = tokenProvider.getUserId(jws);
          List<String> roles = tokenProvider.getRoles(jws);
          var authorities = roles.stream()
              .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
              .toList();
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(userId, null, authorities);
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      } catch (Exception ex) {
        // Invalid/expired token: leave the context unauthenticated so security denies the request.
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7).trim();
    }
    return null;
  }
}
