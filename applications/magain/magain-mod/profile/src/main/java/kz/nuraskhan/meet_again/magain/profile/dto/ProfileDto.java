package kz.nuraskhan.meet_again.magain.profile.dto;

import java.time.LocalDate;

/** What anyone logged in can see about a person. */
public record ProfileDto(
    Long userId,
    String displayName,
    String bio,
    String city,
    LocalDate birthDate,
    String avatarUrl
) {
}
