package com.tegaprive.administration.dto;

import java.math.BigDecimal;

public record RevenueReportDto(
    int periodDays,
    BigDecimal grossFare,
    BigDecimal platformRevenue,
    BigDecimal driverEarnings,
    long bookingCount) {
}
