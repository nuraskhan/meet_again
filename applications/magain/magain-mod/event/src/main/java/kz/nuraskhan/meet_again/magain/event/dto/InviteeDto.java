package kz.nuraskhan.meet_again.magain.event.dto;

import java.time.Instant;
import kz.nuraskhan.meet_again.model.enums.InvitationStatus;

/** One invited person and their answer. */
public record InviteeDto(
    Long invitationId,
    Long userId,
    String displayName,
    InvitationStatus status,
    Instant respondedAt
) {
}
