package com.tallermecanico.api.auth;

import com.tallermecanico.api.user.RoleName;

import java.util.UUID;

public record AuthenticatedUserResponse(UUID id, String username, String fullName, RoleName role) {
}
