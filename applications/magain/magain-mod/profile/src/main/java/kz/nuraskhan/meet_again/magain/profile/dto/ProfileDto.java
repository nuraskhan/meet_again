package kz.nuraskhan.meet_again.magain.profile.dto;

import java.util.List;
import kz.nuraskhan.meet_again.model.enums.Privilege;

/**
 * A person's profile as anyone logged in sees it.
 *
 * <p>Not here on purpose: the e-mail, and the date of birth - others are shown the age only.
 *
 * @param age       whole years, from the date of birth; null if the person did not give one
 * @param avatarUrl the address of the main photo (the first of {@code photos}); null without photos
 * @param photos    all photos, the main one first
 * @param privilege the privilege level of the person
 */
public record ProfileDto(
    Long userId,
    String displayName,
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
    List<PhotoDto> photos,
    List<String> interests,
    List<PromptDto> prompts
) {
}
