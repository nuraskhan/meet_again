package kz.nuraskhan.meet_again.magain.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import kz.nuraskhan.meet_again.model.enums.EventType;
import kz.nuraskhan.meet_again.model.enums.EventVisibility;

/**
 * The invitation form. Only the type is mandatory: title, distance and price left out are taken
 * from the type (see {@link EventTypeDto}).
 *
 * @param proposedTimes the dates the creator offers, moments in UTC ({@code 2026-12-31T15:00:00Z});
 *                      none = to be agreed in the chat
 * @param timeZone      the time zone the dates are meant in, an IANA name from the creator's
 *                      browser (America/New_York); a reminder shows the time in it; may be left out
 * @param visibility    who the event is for; left out = PRIVATE. PUBLIC shows it in the feed, where
 *                      anyone can join
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
    @Size(max = 64) String timeZone,
    EventVisibility visibility,
    @Size(max = 50) List<@NotNull Long> inviteeIds
) {
}
