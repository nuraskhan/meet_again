package kz.nuraskhan.hangelo.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostMessageRequest(@NotBlank @Size(max = 4000) String text) {
}
