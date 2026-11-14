package kz.nuraskhan.hangelo.model.enums;

/**
 * What became of one person's place in an event. A new value is added here AND to the check
 * constraint of {@code invitation.status} (a new migration).
 */
public enum InvitationStatus {
  /** Invited by name, has not answered. */
  PENDING,
  /** Coming: said yes to an invitation, joined by the link, or asked from the feed and was approved. */
  ACCEPTED,
  /** Not coming by their own choice: said no, or left. May join again. */
  DECLINED,
  /** Taken out by the creator of the event. May NOT join again. */
  REMOVED,
  /**
   * Asked to join a public event from the feed; the creator has not answered. Not a guest yet:
   * not in the chat, does not see the event, takes no place in it.
   */
  REQUESTED,
  ;

  /** Is the person part of the event now: invited and not yet answered, or coming. */
  public boolean isIn() {
    return this == PENDING || this == ACCEPTED;
  }
}
