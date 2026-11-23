package com.tegaprive.complaints;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.complaints.dto.ComplaintDto;
import com.tegaprive.complaints.dto.ComplaintRequest;
import com.tegaprive.complaints.dto.ResolveRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

  private final ComplaintService service;

  public ComplaintController(ComplaintService service) {
    this.service = service;
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<ComplaintDto> file(
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody ComplaintRequest request) {
    return ApiResponse.ok(service.file(userId, request));
  }

  @GetMapping("/my")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<ComplaintDto>> myComplaints(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.ok(service.myComplaints(userId));
  }
}
