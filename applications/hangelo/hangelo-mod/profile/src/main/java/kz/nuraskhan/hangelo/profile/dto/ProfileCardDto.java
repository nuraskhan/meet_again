package kz.nuraskhan.hangelo.profile.dto;

/** A person in a list (search results): just enough to recognise them. The full profile is ProfileDto. */
public record ProfileCardDto(
    Long userId,
    String displayName,
    String city,
    String avatarUrl
) {
}
