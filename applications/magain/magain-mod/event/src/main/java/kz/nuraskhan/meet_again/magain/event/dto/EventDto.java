package kz.nuraskhan.meet_again.magain.event.dto;

import java.time.Instant;
import java.util.List;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import kz.nuraskhan.meet_again.model.enums.EventType;

/**
 * An event as its participants see it.
 *
 * @param proposedTimes the dates the creator offers, earliest first; empty = to be agreed
 * @param chatId        the group chat of the event
 * @param shareToken    the secret part of the link to the event; only the creator gets it, null
 *                      for everyone else
 * @param invitees      everyone invited or joined by the link, in the order they appeared
 * @param suggestedAt   when places were last suggested; null = never
 * @param suggestions   the places suggested, best first; empty while never suggested, and when
 *                      nothing suitable was found
 * @param myPreference  what the caller said about themselves for this event - their place and how
 *                      far they travel; null until they say it
 */
public record EventDto(
    Long id,
    EventType eventType,
    String title,
    String description,
    Integer distanceMinMi,
    Integer distanceMaxMi,
    Integer priceMin,
    Integer priceMax,
    List<Instant> proposedTimes,
    EventStatus status,
    Instant createdAt,
    Long chatId,
    String shareToken,
    Long creatorId,
    String creatorName,
    List<InviteeDto> invitees,
    PreferenceDto myPreference,
    Instant suggestedAt,
    List<SuggestionDto> suggestions
) {
}
