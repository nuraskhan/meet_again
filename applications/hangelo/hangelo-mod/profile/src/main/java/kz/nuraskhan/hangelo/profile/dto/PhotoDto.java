package kz.nuraskhan.hangelo.profile.dto;

/**
 * @param id  what remove / make-main take; meaningful only to the owner
 * @param url the server path of the photo: {@code /public/photo/<token>}
 */
public record PhotoDto(Long id, String url) {
}
