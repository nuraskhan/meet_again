package kz.nuraskhan.hangelo.event.dto;

import java.time.Instant;
import java.util.List;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.enums.EventType;

/**
 * An event as anyone with its link sees it, logged in or not.
 *
 * <p>It is what the invitation says and how many people are coming. No ids and no names except the
 * creator's: who the other guests are is for participants only.
 *
 * @param invitedCount people invited or joined by the link
 * @param goingCount   how many of them said yes
 */
public record PublicEventDto(
    EventType eventType,
    String title,
    String description,
    Integer distanceMinMi,
    Integer distanceMaxMi,
    Integer priceMin,
    Integer priceMax,
    List<Instant> proposedTimes,
    EventStatus status,
    String creatorName,
    int invitedCount,
    int goingCount
) {
}
