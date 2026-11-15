package kz.nuraskhan.hangelo.calendar.dto;

import java.time.LocalDate;
import kz.nuraskhan.hangelo.model.enums.AvailabilityKind;

/**
 * Days a friend marked, as their friends see them: whose, what kind, which days, which city.
 * The friend's note is not here on purpose - it is theirs alone.
 */
public record FriendAvailabilityDto(
    Long userId,
    String displayName,
    String avatarUrl,
    AvailabilityKind kind,
    LocalDate fromDate,
    LocalDate toDate,
    String city
) {
}
