package kz.nuraskhan.meet_again.magain.notification.dto;

import java.time.Instant;
import kz.nuraskhan.meet_again.model.enums.NotificationKind;

/**
 * A notification as its recipient gets it - in the list, and in the signal pushed when it appears.
 *
 * @param text    the sentence to show: "Bob invited you to Game night"
 * @param actorId who did it; null when nobody in particular did
 * @param eventId the event it is about, to open it; null when it is not about an event
 * @param read    the person has marked it as read
 */
public record NotificationDto(
    Long id,
    NotificationKind kind,
    String text,
    Long actorId,
    String actorName,
    String actorAvatarUrl,
    Long eventId,
    Instant createdAt,
    boolean read
) {
}
