package ru.formatkoda.polybank.domain.auth;

import lombok.NonNull;

public record JwtToken(
        @NonNull String token
) {
}
