package kz.nuraskhan.hangelo.friend.dto;

import java.time.Instant;

/**
 * A friend request waiting for an answer.
 *
 * @param requestId what accept / decline take
 * @param userId    the other person: the sender of an incoming request, the addressee of an
 *                  outgoing one
 */
public record FriendRequestDto(
    Long requestId,
    Long userId,
    String displayName,
    String avatarUrl,
    Instant createdAt
) {
}
