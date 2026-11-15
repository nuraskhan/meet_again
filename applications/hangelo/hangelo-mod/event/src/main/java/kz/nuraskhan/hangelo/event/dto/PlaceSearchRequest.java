package kz.nuraskhan.hangelo.event.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import kz.nuraskhan.hangelo.model.enums.EventType;

/**
 * Looking for venues from where the caller is.
 *
 * <p>WHY this is a request with a body and not a few more parameters of {@code GET /place/list}:
 * it carries the caller's position. An address with coordinates in it ends up in the logs of every
 * proxy on the way and in the history of the browser; a body does not.
 *
 * @param category  only of this category; null = any
 * @param city      only in this city; null = any
 * @param q         a part of the name or the address; null = any
 * @param latitude  with longitude: the point to measure from. Each venue then says how far it is,
 *                  and the nearest come first. Both null = the best rated first, no distances.
 * @param limit     how many at most; null = the usual page
 */
public record PlaceSearchRequest(
    EventType category,
    @Size(max = 80) String city,
    @Size(max = 100) String q,
    @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @DecimalMin("-180") @DecimalMax("180") Double longitude,
    Integer limit
) {
}
