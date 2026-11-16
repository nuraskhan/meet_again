package kz.nuraskhan.hangelo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** @param code the six digits from the confirmation letter */
public record ConfirmEmailRequest(
    @NotBlank @Pattern(regexp = "\\s*\\d{6}\\s*", message = "The code is six digits") String code) {
}
