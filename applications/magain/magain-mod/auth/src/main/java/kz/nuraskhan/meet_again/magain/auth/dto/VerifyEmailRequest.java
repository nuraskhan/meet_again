package kz.nuraskhan.meet_again.magain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param token the token from the link in the confirmation letter */
public record VerifyEmailRequest(@NotBlank @Size(max = 200) String token) {
}
