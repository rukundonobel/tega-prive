package com.tegaprive.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.users.UserStatus;

/** Idempotently creates the bootstrap super-admin from environment configuration. */
@Configuration
public class DataInitializer {

  @Bean
  CommandLineRunner ensureSuperAdmin(UserRepository userRepository, PasswordEncoder encoder, AdminProperties admin) {
    return args -> {
      if (admin.email() == null || admin.email().isBlank()) {
        return;
      }
      if (userRepository.existsByEmailIgnoreCase(admin.email())) {
        return;
      }
      User user = new User();
      user.setEmail(admin.email().toLowerCase());
      user.setPasswordHash(encoder.encode(admin.password()));
      user.setFirstName("Super");
      user.setLastName("Admin");
      user.setStatus(UserStatus.ACTIVE);
      user.setRoles(Set.of(Role.SUPER_ADMIN));
      userRepository.save(user);
      System.out.println("[TEGA PRIVÉ] Bootstrap super-admin created: " + admin.email());
    };
  }
}
