package kz.nuraskhan.hangelo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 200) String email,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank @Size(max = 100) String displayName
) {
}
