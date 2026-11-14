package kz.nuraskhan.meet_again.magain.chat.dto;

public record ChatMemberDto(
    Long userId,
    String displayName,
    String avatarUrl
) {
}
