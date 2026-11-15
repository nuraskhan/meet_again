package kz.nuraskhan.hangelo.calendar.dto;

import java.time.LocalDate;
import kz.nuraskhan.hangelo.model.enums.AvailabilityKind;

/**
 * Days the caller marked in their own calendar.
 *
 * @param toDate the last day, included
 * @param city   where to, for a TRIP; null otherwise
 * @param note   the caller's own note; never shown to anyone else
 */
public record AvailabilityDto(
    Long id,
    AvailabilityKind kind,
    LocalDate fromDate,
    LocalDate toDate,
    String city,
    String note
) {
}
