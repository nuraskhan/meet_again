package kz.nuraskhan.hangelo.calendar.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import kz.nuraskhan.hangelo.model.enums.AvailabilityKind;

/**
 * @param toDate the last day, included; the same as fromDate for one day
 * @param city   where to; required for a TRIP, ignored otherwise
 */
public record SaveAvailabilityRequest(
    @NotNull AvailabilityKind kind,
    @NotNull LocalDate fromDate,
    @NotNull LocalDate toDate,
    @Size(max = 80) String city,
    @Size(max = 200) String note
) {
}
