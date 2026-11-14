package kz.nuraskhan.meet_again.magain.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** The whole editable part of a profile: a field left out becomes empty. */
public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 1000) String bio,
    @Size(max = 100) String city,
    LocalDate birthDate,
    @Size(max = 500) String avatarUrl
) {
}
