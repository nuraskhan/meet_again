package kz.nuraskhan.meet_again.magain.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A question the person chose and their answer: "My perfect weekend is..." - "A hike and a nap." */
public record PromptDto(
    @NotBlank @Size(max = 120) String question,
    @NotBlank @Size(max = 300) String answer
) {
}
