package kz.nuraskhan.hangelo.friend.dto;

import java.time.Instant;

public record FriendDto(
    Long userId,
    String displayName,
    String city,
    String avatarUrl,
    Instant friendsSince
) {
}
