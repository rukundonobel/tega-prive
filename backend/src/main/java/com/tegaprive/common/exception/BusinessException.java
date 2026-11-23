package com.tegaprive.common.exception;

/** Base class for expected, business-rule violations mapped to specific HTTP statuses. */
public class BusinessException extends RuntimeException {

  public BusinessException(String message) {
    super(message);
  }
}
