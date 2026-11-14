package kz.nuraskhan.meet_again.magain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {
}
