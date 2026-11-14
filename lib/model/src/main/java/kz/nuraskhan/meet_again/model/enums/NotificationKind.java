package kz.nuraskhan.meet_again.model.enums;

/**
 * What a notification is about. The comment of each value says who receives it.
 *
 * <p>A new kind is added here AND to the check constraint of {@code notification.kind} (a new
 * migration), and gets its sentence in NotificationService.
 */
public enum NotificationKind {
  /** To the addressee: someone wants to be their friend. */
  FRIEND_REQUEST,
  /** To the one who asked: the other person said yes. */
  FRIEND_ACCEPTED,
  /** To a person invited by name to an event. */
  EVENT_INVITATION,
  /** To the creator: an invited person said yes. */
  INVITATION_ACCEPTED,
  /** To the creator: an invited person said no. */
  INVITATION_DECLINED,
  /** To the creator: someone joined by the link or from the feed. */
  GUEST_JOINED,
  /** To the creator: a guest who was coming left. */
  GUEST_LEFT,
  /** To a guest: the creator removed them from the event. */
  REMOVED_FROM_EVENT,
  /** To every guest: the creator cancelled the event. */
  EVENT_CANCELLED,
}
