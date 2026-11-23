package com.tegaprive.common;

import java.util.Map;

/**
 * Standard API response envelope used by all controllers.
 *
 * @param success whether the request succeeded
 * @param data    the payload (null on error)
 * @param message optional human-readable message
 * @param errors  optional field-level error map (validation failures)
 */
public record ApiResponse<T>(boolean success, T data, String message, Map<String, String> errors) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null, null);
  }

  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(true, data, message, null);
  }

  public static <T> ApiResponse<T> error(String message, Map<String, String> errors) {
    return new ApiResponse<>(false, null, message, errors);
  }
}
