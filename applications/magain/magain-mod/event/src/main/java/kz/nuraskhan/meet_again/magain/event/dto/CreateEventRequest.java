package kz.nuraskhan.meet_again.magain.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import kz.nuraskhan.meet_again.model.enums.EventType;

/**
 * The invitation form. Only the type is mandatory: title, distance and price left out are taken
 * from the type (see {@link EventTypeDto}).
 *
 * @param proposedTimes the dates the creator offers, moments in UTC ({@code 2026-12-31T15:00:00Z});
 *                      none = to be agreed in the chat
 * @param inviteeIds    friends of the caller; may be empty - then people come by the link of the
 *                      event
 */
public record CreateEventRequest(
    @NotNull EventType eventType,
    @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Min(0) Integer distanceMinMi,
    @Min(0) Integer distanceMaxMi,
    @Min(0) Integer priceMin,
    @Min(0) Integer priceMax,
    @Size(max = 10) List<@NotNull Instant> proposedTimes,
    @Size(max = 50) List<@NotNull Long> inviteeIds
) {
}
