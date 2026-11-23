package com.tegaprive.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.auth.dto.AuthResponse;
import com.tegaprive.auth.dto.LoginRequest;
import com.tegaprive.auth.dto.RefreshRequest;
import com.tegaprive.auth.dto.RegisterRequest;
import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.users.UserRepository;

/**
 * Integration test of the auth flow against an in-memory H2 database (test profile).
 * Exercises registration, duplicate prevention, login, bad credentials and refresh.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

  @Autowired
  private AuthService authService;

  @Autowired
  private UserRepository userRepository;

  private RegisterRequest registerRequest(String email) {
    return new RegisterRequest("Claudine", "Uwase", email, "+250788000111", "password123");
  }

  @Test
  void registersPassengerAndIssuesTokens() {
    AuthResponse response = authService.register(registerRequest("claudine@example.com"));

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.user().email()).isEqualTo("claudine@example.com");
    assertThat(response.user().roles()).contains("PASSENGER");
    assertThat(userRepository.existsByEmailIgnoreCase("claudine@example.com")).isTrue();
  }

  @Test
  void preventsDuplicateRegistration() {
    authService.register(registerRequest("dup@example.com"));
    assertThatThrownBy(() -> authService.register(registerRequest("dup@example.com")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void logsInWithValidCredentials() {
    authService.register(registerRequest("login@example.com"));
    AuthResponse response = authService.login(new LoginRequest("login@example.com", "password123"));

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.user().email()).isEqualTo("login@example.com");
  }

  @Test
  void rejectsInvalidCredentials() {
    authService.register(registerRequest("wrong@example.com"));
    assertThatThrownBy(() -> authService.login(new LoginRequest("wrong@example.com", "bad-password")))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void refreshesTokens() {
    AuthResponse initial = authService.register(registerRequest("refresh@example.com"));
    AuthResponse refreshed = authService.refresh(new RefreshRequest(initial.refreshToken()));

    assertThat(refreshed.accessToken()).isNotBlank();
    assertThat(refreshed.user().email()).isEqualTo("refresh@example.com");
  }
}
