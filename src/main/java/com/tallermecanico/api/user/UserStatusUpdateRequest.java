package com.tallermecanico.api.user;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(@NotNull(message = "El estado es obligatorio.") Boolean active) {
}
