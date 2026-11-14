package kz.nuraskhan.meet_again.magain.profile.dto;

import java.time.LocalDate;

/** The caller's own profile: the public part plus what only they may see. */
public record MyProfileDto(
    Long userId,
    String email,
    String displayName,
    String bio,
    String city,
    LocalDate birthDate,
    String avatarUrl
) {
}
