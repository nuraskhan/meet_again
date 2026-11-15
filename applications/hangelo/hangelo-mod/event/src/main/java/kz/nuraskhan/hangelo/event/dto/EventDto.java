package kz.nuraskhan.hangelo.event.dto;

import java.time.Instant;
import java.util.List;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.model.enums.EventVisibility;

/**
 * An event as its participants see it.
 *
 * @param proposedTimes the dates the creator offers, earliest first; empty = to be agreed
 * @param visibility    PRIVATE - only the invited; PUBLIC - shown in the feed, anyone can join
 * @param chatId        the group chat of the event
 * @param shareToken    the secret part of the link to the event; only the creator gets it, null
 *                      for everyone else
 * @param invitees      everyone invited or joined by the link, in the order they appeared
 * @param suggestedAt   when places were last suggested; null = never
 * @param suggestions   the places suggested, best first; empty while never suggested, and when
 *                      nothing suitable was found
 * @param myPreference  what the caller said about themselves for this event - their place and how
 *                      far they travel; null until they say it
 * @param place         the venue the creator chose for the event; null = not chosen
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
    EventVisibility visibility,
    Instant createdAt,
    Long chatId,
    String shareToken,
    Long creatorId,
    String creatorName,
    List<InviteeDto> invitees,
    PreferenceDto myPreference,
    Instant suggestedAt,
    List<SuggestionDto> suggestions,
    PlaceDto place
) {
}
