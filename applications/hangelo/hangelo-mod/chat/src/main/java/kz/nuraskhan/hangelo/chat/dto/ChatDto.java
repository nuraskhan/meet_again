package kz.nuraskhan.hangelo.chat.dto;

import java.time.Instant;

/** @param memberCount people in the chat now, the caller included */
public record ChatDto(
    Long id,
    String title,
    Instant createdAt,
    Integer memberCount
) {
}
