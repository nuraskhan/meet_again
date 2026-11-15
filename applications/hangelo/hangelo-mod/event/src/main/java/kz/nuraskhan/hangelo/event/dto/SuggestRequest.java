package kz.nuraskhan.hangelo.event.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Where the caller is right now, sent together with "Suggest places" - so that one press is
 * enough for someone who has not put their location on the map yet. Both or neither.
 *
 * <p>Used only if the caller has no saved location for the event; it is then saved as theirs,
 * exactly as if they had filled the location form.
 */
public record SuggestRequest(
    @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @DecimalMin("-180") @DecimalMax("180") Double longitude
) {
}
