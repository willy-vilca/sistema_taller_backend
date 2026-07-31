package com.tallermecanico.api.service;

import java.math.BigDecimal;

public record DashboardStatsResponse(long clients, long vehicles, long services, BigDecimal revenue) {
}
