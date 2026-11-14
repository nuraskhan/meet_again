package kz.nuraskhan.hangelo.chat.dto;

public record ChatMemberDto(
    Long userId,
    String displayName,
    String avatarUrl
) {
}
