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
 */
public record EventDto(
    Long id,
    EventType eventType,
    String title,
    String description,
    Integer distanceMinKm,
    Integer distanceMaxKm,
    Integer priceMin,
    Integer priceMax,
    List<Instant> proposedTimes,
    EventStatus status,
    Instant createdAt,
    Long chatId,
    String shareToken,
    Long creatorId,
    String creatorName,
    List<InviteeDto> invitees
) {
}
