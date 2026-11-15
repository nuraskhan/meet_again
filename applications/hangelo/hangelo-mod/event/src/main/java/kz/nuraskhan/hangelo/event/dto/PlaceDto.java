package kz.nuraskhan.hangelo.event.dto;

import kz.nuraskhan.hangelo.model.enums.EventType;

/**
 * A venue: a place people can meet at.
 *
 * @param category    what one does there - the same values as the type of an event
 * @param city        the city it is in; null if not known
 * @param rating      how good it is, 0 to 5
 * @param reviewCount how many reviews the rating rests on; null = unknown
 * @param sample      an invented demonstration place, not a real business
 */
public record PlaceDto(
    Long id,
    String name,
    EventType category,
    String description,
    String address,
    String city,
    Double latitude,
    Double longitude,
    Double rating,
    Integer reviewCount,
    Integer priceMin,
    Integer priceMax,
    String website,
    String phone,
    boolean sample
) {
}
