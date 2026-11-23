package com.tegaprive.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/** Pure unit test — no Spring context, no database. Verifies JWT issue/parse/tamper logic. */
class JwtTokenProviderTest {

  private JwtTokenProvider provider;
  private User user;

  @BeforeEach
  void setUp() {
    JwtProperties properties = new JwtProperties(
        "test-secret-at-least-thirty-two-bytes-long-aaaaaaaaaaaa", 30, 7);
    provider = new JwtTokenProvider(properties);

    user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail("passenger@example.com");
    user.setFirstName("Claudine");
    user.setLastName("Uwase");
    user.setStatus(UserStatus.ACTIVE);
    user.setRoles(Set.of(Role.PASSENGER));
  }

  @Test
  void generatesAndParsesAccessToken() {
    String token = provider.generateAccessToken(user);
    assertThat(token).isNotBlank();

    Jws<Claims> jws = provider.parse(token);
    assertThat(jws.getPayload().getSubject()).isEqualTo(user.getId().toString());
    assertThat(provider.getTokenType(jws)).isEqualTo("access");
    assertThat(provider.getRoles(jws)).contains("PASSENGER");
    assertThat(jws.getPayload().get("email", String.class)).isEqualTo("passenger@example.com");
  }

  @Test
  void generatesRefreshTokenWithCorrectType() {
    String token = provider.generateRefreshToken(user);
    Jws<Claims> jws = provider.parse(token);
    assertThat(provider.getTokenType(jws)).isEqualTo("refresh");
    assertThat(jws.getPayload().getSubject()).isEqualTo(user.getId().toString());
  }

  @Test
  void rejectsTamperedToken() {
    String token = provider.generateAccessToken(user);
    String tampered = token.substring(0, token.length() - 4) + "AAAA";
    assertThatThrownBy(() -> provider.parse(tampered)).isInstanceOf(Exception.class);
  }

  @Test
  void rejectsTokenSignedWithDifferentSecret() {
    String token = provider.generateAccessToken(user);
    JwtTokenProvider other = new JwtTokenProvider(
        new JwtProperties("a-different-secret-at-least-thirty-two-bytes-long-bbbbbbbb", 30, 7));
    assertThatThrownBy(() -> other.parse(token)).isInstanceOf(Exception.class);
  }
}
