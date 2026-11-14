package kz.nuraskhan.meet_again.magain.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import kz.nuraskhan.meet_again.model.enums.EventType;

/**
 * The invitation form. Only the type and the invitees are mandatory: title, distance and price left
 * out are taken from the type (see {@link EventTypeDto}).
 *
 * @param startsAt   when; a moment in UTC ({@code 2026-12-31T15:00:00Z}); may be left out
 * @param inviteeIds friends of the caller
 */
public record CreateEventRequest(
    @NotNull EventType eventType,
    @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Min(0) Integer distanceMinKm,
    @Min(0) Integer distanceMaxKm,
    @Min(0) Integer priceMin,
    @Min(0) Integer priceMax,
    Instant startsAt,
    @NotEmpty @Size(max = 50) List<@NotNull Long> inviteeIds
) {
}
