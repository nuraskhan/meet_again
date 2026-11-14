package kz.nuraskhan.meet_again.magain.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything a person writes about themselves. The whole of it is replaced on every save: a field
 * left out becomes empty, a list left out becomes an empty list.
 *
 * <p>Not here on purpose, so that it cannot be set by the person:
 * <ul>
 *   <li>the photos - they are uploaded as files ({@code POST /profile/me/photo});
 *   <li>the privilege level - it is the base of paid plans and is set by us.
 * </ul>
 *
 * @param interests short labels, in the person's order
 * @param prompts   questions the person chose, with their answers
 */
public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String displayName,
    @Past LocalDate birthDate,
    @Size(max = 40) String pronouns,
    @Size(max = 100) String city,
    @Size(max = 100) String hometown,
    @Size(max = 120) String occupation,
    @Size(max = 120) String education,
    @Size(max = 200) String languages,
    @Size(max = 1000) String bio,
    @Size(max = 10) List<@NotBlank @Size(max = 30) String> interests,
    @Size(max = 3) List<@NotNull @Valid PromptDto> prompts
) {
}
