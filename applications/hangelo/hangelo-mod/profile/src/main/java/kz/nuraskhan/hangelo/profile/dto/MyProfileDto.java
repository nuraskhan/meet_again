package kz.nuraskhan.hangelo.profile.dto;

import java.time.LocalDate;
import java.util.List;
import kz.nuraskhan.hangelo.model.enums.Privilege;

/**
 * The caller's own profile: everything others see (ProfileDto) plus what only they may see.
 *
 * @param emailVerified the person confirmed the address by the link we sent
 * @param birthDate     as the person entered it; others see only {@code age}
 * @param privilege     the privilege level; the person cannot change it
 * @param maxPhotos     how many photos that level allows
 */
public record MyProfileDto(
    Long userId,
    String email,
    boolean emailVerified,
    String displayName,
    LocalDate birthDate,
    Integer age,
    String pronouns,
    String city,
    String hometown,
    String occupation,
    String education,
    String languages,
    String bio,
    String avatarUrl,
    Privilege privilege,
    int maxPhotos,
    List<PhotoDto> photos,
    List<String> interests,
    List<PromptDto> prompts
) {
}
