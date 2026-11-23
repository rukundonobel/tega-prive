package com.tegaprive.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

/** Creates and validates JWT access/refresh tokens (HS256). Stateless — no token store. */
@Component
public class JwtTokenProvider {

  private static final String TYPE_ACCESS = "access";
  private static final String TYPE_REFRESH = "refresh";

  private final SecretKey key;
  private final long accessExpirationMs;
  private final long refreshExpirationMs;

  public JwtTokenProvider(JwtProperties properties) {
    byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      throw new IllegalStateException("TP_JWT_SECRET must be at least 32 bytes for HS256.");
    }
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessExpirationMs = properties.accessExpirationMin() * 60_000L;
    this.refreshExpirationMs = properties.refreshExpirationDays() * 24L * 60L * 60_000L;
  }

  public String generateAccessToken(User user) {
    Date now = new Date();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("roles", user.getRoles().stream().map(Role::name).toList())
        .claim("type", TYPE_ACCESS)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + accessExpirationMs))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public String generateRefreshToken(User user) {
    Date now = new Date();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("type", TYPE_REFRESH)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + refreshExpirationMs))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  /** Parses and verifies the signature; throws on invalid/expired tokens. */
  public Jws<Claims> parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
  }

  public UUID getUserId(Jws<Claims> jws) {
    return UUID.fromString(jws.getPayload().getSubject());
  }

  public String getTokenType(Jws<Claims> jws) {
    return jws.getPayload().get("type", String.class);
  }

  @SuppressWarnings("unchecked")
  public List<String> getRoles(Jws<Claims> jws) {
    Object roles = jws.getPayload().get("roles");
    return roles instanceof List<?> list ? list.stream().map(Object::toString).toList() : List.of();
  }
}
