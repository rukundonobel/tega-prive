package com.tegaprive.administration.dto;

import java.math.BigDecimal;

public record DashboardStatsDto(
    long totalUsers,
    long totalDrivers,
    long verifiedDrivers,
    long totalVehicles,
    long approvedVehicles,
    long totalJourneys,
    long activeJourneys,
    long totalBookings,
    long confirmedBookings,
    BigDecimal totalPlatformRevenue,
    BigDecimal totalDriverEarnings,
    long pendingComplaints,
    long pendingPayouts) {
}
