package kz.nuraskhan.hangelo.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One page opened.
 *
 * @param visitorId a random id the page made up and keeps in the browser; letters, digits, dashes
 * @param path      the pattern of the page ("/plans/:eventId"), not its address: an address may
 *                  carry a secret
 */
public record VisitRequest(
    @NotBlank @Size(min = 8, max = 64) @Pattern(regexp = "[A-Za-z0-9-]+") String visitorId,
    @NotBlank @Size(max = 120) @Pattern(regexp = "/[A-Za-z0-9/:_.-]*") String path
) {
}
