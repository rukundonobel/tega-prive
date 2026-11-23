package com.tegaprive.auth;

import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.auth.dto.AuthResponse;
import com.tegaprive.auth.dto.LoginRequest;
import com.tegaprive.auth.dto.RefreshRequest;
import com.tegaprive.auth.dto.RegisterRequest;
import com.tegaprive.auth.dto.UserDto;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.security.JwtTokenProvider;
import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.users.UserStatus;

/** Authentication: registration (passenger), login, token refresh, current user. */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;
  private final AuthenticationManager authenticationManager;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      JwtTokenProvider tokenProvider, AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenProvider = tokenProvider;
    this.authenticationManager = authenticationManager;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new ConflictException("An account with this email already exists");
    }
    User user = new User();
    user.setEmail(request.email().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setPhone(request.phone());
    user.setStatus(UserStatus.ACTIVE);
    user.setRoles(Set.of(Role.PASSENGER));
    userRepository.save(user);
    return toAuthResponse(user);
  }

  public AuthResponse login(LoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
    User user = userRepository.findByEmailIgnoreCase(authentication.getName())
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return toAuthResponse(user);
  }

  @Transactional
  public AuthResponse refresh(RefreshRequest request) {
    var jws = tokenProvider.parse(request.refreshToken());
    if (!"refresh".equals(tokenProvider.getTokenType(jws))) {
      throw new BadRequestException("Invalid refresh token");
    }
    UUID userId = tokenProvider.getUserId(jws);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return toAuthResponse(user);
  }

  @Transactional(readOnly = true)
  public UserDto getUserById(UUID id) {
    return UserMapper.toDto(userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
  }

  private AuthResponse toAuthResponse(User user) {
    return new AuthResponse(
        tokenProvider.generateAccessToken(user),
        tokenProvider.generateRefreshToken(user),
        UserMapper.toDto(user));
  }
}
