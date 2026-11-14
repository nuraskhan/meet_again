package kz.nuraskhan.meet_again.magain.auth.dto;

/**
 * @param token  goes into the header {@code Authorization: Bearer <token>} of every other request
 * @param userId {@code user_account.id} of the logged-in user
 */
public record AuthResponse(String token, Long userId) {
}
