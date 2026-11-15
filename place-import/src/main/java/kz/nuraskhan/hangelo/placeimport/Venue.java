package kz.nuraskhan.hangelo.placeimport;

/**
 * One venue as the directory gave it.
 *
 * @param externalId  its id in the directory: the same venue loaded again is the same row
 * @param rating      0 to 5; null = the directory gave none
 * @param reviewCount how many reviews stand behind the rating; null = not given
 */
public record Venue(
    String externalId,
    String name,
    String address,
    double latitude,
    double longitude,
    Double rating,
    Integer reviewCount
) {
}
