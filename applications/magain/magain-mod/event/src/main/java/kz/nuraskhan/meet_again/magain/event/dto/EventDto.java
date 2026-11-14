package kz.nuraskhan.meet_again.magain.event.dto;

import java.time.Instant;
import java.util.List;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import kz.nuraskhan.meet_again.model.enums.EventType;

/**
 * @param chatId   the group chat of the event
 * @param invitees everyone invited, in the order they were invited
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
    Instant startsAt,
    EventStatus status,
    Instant createdAt,
    Long chatId,
    Long creatorId,
    String creatorName,
    List<InviteeDto> invitees
) {
}
