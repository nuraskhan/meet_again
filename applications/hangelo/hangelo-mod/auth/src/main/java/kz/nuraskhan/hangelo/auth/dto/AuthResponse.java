package kz.nuraskhan.hangelo.auth.dto;

/**
 * @param token         goes into the header {@code Authorization: Bearer <token>} of every other request
 * @param userId        {@code user_account.id} of the logged-in user
 * @param emailVerified false = the token opens nothing yet but the confirmation of the e-mail: the
 *                      page shows "check your inbox" instead of the application
 */
public record AuthResponse(String token, Long userId, boolean emailVerified) {
}
