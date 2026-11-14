package kz.nuraskhan.meet_again.magain.event.dto;

import kz.nuraskhan.meet_again.model.enums.EventType;

/** A type of event with the values its invitation form is pre-filled with. */
public record EventTypeDto(
    EventType eventType,
    String defaultTitle,
    int defaultDistanceMinKm,
    int defaultDistanceMaxKm,
    int defaultPriceMin,
    int defaultPriceMax
) {

  public static EventTypeDto of(EventType type) {
    return new EventTypeDto(type, type.defaultTitle, type.defaultDistanceMinKm,
        type.defaultDistanceMaxKm, type.defaultPriceMin, type.defaultPriceMax);
  }
}
