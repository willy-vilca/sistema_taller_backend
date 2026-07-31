package com.tallermecanico.api.user;

import java.util.UUID;

public record UserSummaryResponse(UUID id, String fullName, String username) {
}
