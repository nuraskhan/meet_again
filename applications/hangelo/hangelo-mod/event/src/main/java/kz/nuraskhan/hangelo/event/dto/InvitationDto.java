package kz.nuraskhan.hangelo.event.dto;

/**
 * An invitation the caller has not answered yet.
 *
 * @param invitationId what accept / decline take
 * @param event        the form the caller is invited with
 */
public record InvitationDto(Long invitationId, EventDto event) {
}
