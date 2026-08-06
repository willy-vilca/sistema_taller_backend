package com.tallermecanico.api.user;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toResponse(SystemUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName(),
                user.isScheduleNotificationsEnabled(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    public static UserSummaryResponse toSummary(SystemUser user) {
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getUsername());
    }
}
