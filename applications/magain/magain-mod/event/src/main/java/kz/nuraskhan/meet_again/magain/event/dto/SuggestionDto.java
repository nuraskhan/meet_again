package kz.nuraskhan.meet_again.magain.event.dto;

import kz.nuraskhan.meet_again.model.enums.EventType;

/**
 * A place suggested for an event, with what one needs to decide.
 *
 * <p>The distances were measured when the suggestions were computed, from the people whose location
 * was known then, as the crow flies. They do not say who is how far - only the average, the
 * farthest, and the caller's own.
 *
 * @param rank                   1 = the best
 * @param rating                 how good the place is, 0 to 5
 * @param priceMin               expected spending per person
 * @param averageDistanceMi      the mean over those people
 * @param farthestDistanceMi     for the one who is farthest
 * @param withinEveryonesReach   each of those people is within their own travel radius
 * @param fitsBudget             the prices of the place overlap the budget of the event
 * @param participantsConsidered how many people's locations the suggestion is based on
 * @param myDistanceMi           from the caller's own saved location, now; null if they gave none
 * @param sample                 an invented demonstration place, not a real business
 */
public record SuggestionDto(
    Integer rank,
    Long placeId,
    String name,
    EventType category,
    String description,
    String address,
    Double latitude,
    Double longitude,
    Double rating,
    Integer priceMin,
    Integer priceMax,
    String website,
    String phone,
    Double averageDistanceMi,
    Double farthestDistanceMi,
    boolean withinEveryonesReach,
    boolean fitsBudget,
    Integer participantsConsidered,
    Double myDistanceMi,
    boolean sample
) {
}
