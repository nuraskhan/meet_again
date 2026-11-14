package kz.nuraskhan.hangelo.event.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What a guest says about themselves for one event. The whole thing is replaced on every save.
 *
 * <p>The place is given as coordinates (latitude and longitude together - from the device, with the
 * person's permission), as a typed text, or both. At least one of the two is required.
 *
 * @param travelRadiusMi the farthest the person is ready to go, miles
 */
public record SavePreferenceRequest(
    @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @DecimalMin("-180") @DecimalMax("180") Double longitude,
    @Size(max = 200) String locationText,
    @NotNull @Min(1) @Max(500) Integer travelRadiusMi
) {
}
