package com.tallermecanico.api.user;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toResponse(SystemUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().getName(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    public static UserSummaryResponse toSummary(SystemUser user) {
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getUsername());
    }
}
