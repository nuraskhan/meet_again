package kz.nuraskhan.meet_again.model.enums;

public enum FriendshipStatus {
  /** The request is sent and not answered. */
  PENDING,
  /** They are friends. */
  ACCEPTED,
  /** The addressee said no. */
  DECLINED,
  /** They were friends, one of them ended it. */
  REMOVED,
}
