package kz.nuraskhan.hangelo.friend.dto;

import jakarta.validation.constraints.NotNull;

/** @param userId whom the caller wants as a friend */
public record SendFriendRequest(@NotNull Long userId) {
}
