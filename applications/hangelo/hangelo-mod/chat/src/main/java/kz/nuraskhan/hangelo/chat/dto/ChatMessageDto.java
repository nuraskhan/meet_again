package kz.nuraskhan.hangelo.chat.dto;

import java.time.Instant;

/**
 * @param replyTo the message this one answers, in short; null = it answers nothing
 */
public record ChatMessageDto(
    Long id,
    Long authorId,
    String authorName,
    String text,
    Instant createdAt,
    ReplyDto replyTo
) {

  /**
   * The answered message, as shown above the answer: who wrote it and its beginning.
   *
   * @param text the beginning of the answered message, cut to a line or two
   */
  public record ReplyDto(
      Long id,
      Long authorId,
      String authorName,
      String text
  ) {
  }
}
