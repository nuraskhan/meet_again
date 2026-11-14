package kz.nuraskhan.meet_again.magain.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The whole editable text part of a profile: a field left out becomes empty.
 *
 * <p>The picture is not here on purpose. It cannot be set to an address: it is uploaded as a file
 * ({@code POST /profile/me/avatar}).
 */
public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 1000) String bio,
    @Size(max = 100) String city,
    LocalDate birthDate
) {
}
