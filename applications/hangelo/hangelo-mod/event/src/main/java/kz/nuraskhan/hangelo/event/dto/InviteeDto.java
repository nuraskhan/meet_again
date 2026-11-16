package kz.nuraskhan.hangelo.event.dto;

import java.time.Instant;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;

/**
 * One guest, their answer, and the part of their preference the other participants may see.
 *
 * <p>Coordinates are never here: where exactly a person is, is theirs alone. The others see the
 * place the person typed (if any) and the fact that a location was given.
 *
 * @param tag            a word we put by the name ("Team"); null = none
 * @param travelRadiusMi the farthest the guest is ready to go, miles; null = not said yet
 * @param locationText   the place as the guest typed it; null if they shared coordinates only or
 *                       said nothing
 * @param locationShared the guest shared the location of their device
 * @param noShow         the creator marked, after the event, that this guest said "coming" and
 *                       did not come
 */
public record InviteeDto(
    Long invitationId,
    Long userId,
    String displayName,
    String tag,
    InvitationStatus status,
    Instant respondedAt,
    Integer travelRadiusMi,
    String locationText,
    boolean locationShared,
    boolean noShow
) {
}
