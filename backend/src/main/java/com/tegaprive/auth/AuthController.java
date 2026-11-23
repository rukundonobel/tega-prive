package com.tegaprive.auth;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tegaprive.auth.dto.AuthResponse;
import com.tegaprive.auth.dto.LoginRequest;
import com.tegaprive.auth.dto.RefreshRequest;
import com.tegaprive.auth.dto.RegisterRequest;
import com.tegaprive.auth.dto.UserDto;
import com.tegaprive.common.ApiResponse;

import jakarta.validation.Valid;

/** Public authentication endpoints. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(authService.register(request)));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserDto>> me(@AuthenticationPrincipal UUID userId) {
    return ResponseEntity.ok(ApiResponse.ok(authService.getUserById(userId)));
  }
}
