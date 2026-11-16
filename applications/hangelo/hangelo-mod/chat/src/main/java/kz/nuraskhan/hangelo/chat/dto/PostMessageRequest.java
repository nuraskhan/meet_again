package kz.nuraskhan.hangelo.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param replyToId the message being answered, of the same chat; absent = an ordinary message
 */
public record PostMessageRequest(@NotBlank @Size(max = 4000) String text, Long replyToId) {
}
