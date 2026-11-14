package kz.nuraskhan.meet_again.magain.chat.dto;

import java.time.Instant;

public record ChatMessageDto(
    Long id,
    Long authorId,
    String authorName,
    String text,
    Instant createdAt
) {
}
