package com.tallermecanico.api.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        RoleName role,
        boolean scheduleNotificationsEnabled,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt
) {
}
