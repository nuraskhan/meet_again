package kz.nuraskhan.hangelo.chat.dto;

import java.time.Instant;
import kz.nuraskhan.hangelo.model.enums.EventType;

/**
 * @param memberCount people in the chat now, the caller included
 * @param eventId     the event the chat belongs to, so a page can lead from the chat to its plan;
 *                    null for a chat without an event
 * @param eventType   the kind of that event (FOOD, WALK, ...), for its icon; null with eventId
 */
public record ChatDto(
    Long id,
    String title,
    Instant createdAt,
    Integer memberCount,
    Long eventId,
    EventType eventType
) {
}
