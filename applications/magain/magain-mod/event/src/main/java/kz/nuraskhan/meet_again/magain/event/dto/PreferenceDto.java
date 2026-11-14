package kz.nuraskhan.meet_again.magain.event.dto;

/**
 * The caller's own preference for an event, exactly as they saved it - coordinates included.
 * Nobody else gets the coordinates: see {@link InviteeDto}.
 */
public record PreferenceDto(
    Double latitude,
    Double longitude,
    String locationText,
    Integer travelRadiusMi
) {
}
