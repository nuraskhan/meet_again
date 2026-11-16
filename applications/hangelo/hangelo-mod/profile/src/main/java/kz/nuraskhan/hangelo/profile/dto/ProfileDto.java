package kz.nuraskhan.hangelo.profile.dto;

import kz.nuraskhan.hangelo.reliability.ReliabilityDto;

import java.util.List;
import kz.nuraskhan.hangelo.model.enums.Privilege;

/**
 * A person's profile as anyone logged in sees it.
 *
 * <p>Not here on purpose: the e-mail, and the date of birth - others are shown the age only.
 *
 * @param tag       a word we put by the name ("Team"); null = none
 * @param age       whole years, from the date of birth; null if the person did not give one
 * @param avatarUrl the address of the main photo (the first of {@code photos}); null without photos
 * @param photos    all photos, the main one first
 * @param privilege the privilege level of the person
 * @param reliability how reliable the person is, counted from their history of events
 */
public record ProfileDto(
    Long userId,
    String displayName,
    String tag,
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
    List<PromptDto> prompts,
    ReliabilityDto reliability
) {
}
