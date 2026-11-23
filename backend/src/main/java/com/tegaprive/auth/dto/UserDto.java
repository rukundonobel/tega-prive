package com.tegaprive.auth.dto;

import java.util.List;

/** Public user projection — never exposes password hash or sensitive data. */
public record UserDto(
    String id,
    String email,
    String firstName,
    String lastName,
    String phone,
    List<String> roles,
    String status) {
}
