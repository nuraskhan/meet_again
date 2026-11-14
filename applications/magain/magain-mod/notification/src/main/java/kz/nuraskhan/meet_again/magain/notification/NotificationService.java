package kz.nuraskhan.meet_again.magain.notification;

import java.time.Instant;
import kz.nuraskhan.meet_again.magain.notification.dto.NotificationDto;
import kz.nuraskhan.meet_again.magain.notification.signal.SignalHub;
import kz.nuraskhan.meet_again.model.entity.Event;
import kz.nuraskhan.meet_again.model.entity.Notification;
import kz.nuraskhan.meet_again.model.entity.Profile;
import kz.nuraskhan.meet_again.model.enums.NotificationKind;
import kz.nuraskhan.meet_again.model.repository.EventRepository;
import kz.nuraskhan.meet_again.model.repository.NotificationRepository;
import kz.nuraskhan.meet_again.model.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells a person that something happened: writes it down as a {@link Notification} and signals the
 * pages the person has open, so it shows at once.
 *
 * <p>The other modules call {@link #tell} at the place where the thing happens, inside the same
 * transaction: if the thing is rolled back, so is the notification, and no signal goes out
 * (SignalHub sends after the commit).
 *
 * <p>The sentence a person reads is composed here, in one place, from the kind, the name of who
 * did it and the title of the event - and stored. A later change of a name or a title does not
 * rewrite what was said.
 */
@Service
public class NotificationService {

  /** The type of the signal that carries a new notification. */
  public static final String SIGNAL = "notification";

  private final NotificationRepository notificationRepository;
  private final ProfileRepository profileRepository;
  private final EventRepository eventRepository;
  private final SignalHub signalHub;

  public NotificationService(NotificationRepository notificationRepository,
                             ProfileRepository profileRepository,
                             EventRepository eventRepository,
                             SignalHub signalHub) {
    this.notificationRepository = notificationRepository;
    this.profileRepository = profileRepository;
    this.eventRepository = eventRepository;
    this.signalHub = signalHub;
  }

  /**
   * Tells {@code recipientId} that {@code actorId} did something.
   *
   * <p>Nobody is told about what they did themselves: a call where the recipient is the actor does
   * nothing, so callers need not check.
   *
   * @param eventId the event it is about; null for what is not about an event (friend requests)
   */
  @Transactional
  public void tell(Long recipientId, NotificationKind kind, Long actorId, Long eventId) {
    if (recipientId.equals(actorId)) {
      return;
    }

    Profile actor = actorId == null ? null : profileRepository.findById(actorId).orElse(null);
    Event event = eventId == null ? null : eventRepository.findById(eventId).orElse(null);

    Notification notification = new Notification();
    notification.userAccountId = recipientId;
    notification.kind = kind;
    notification.actorUserAccountId = actorId;
    notification.eventId = eventId;
    notification.text = sentence(kind, actor == null ? "Someone" : actor.displayName,
        event == null ? "a plan" : event.title);
    notification.createdAt = Instant.now();
    notification = notificationRepository.save(notification);

    signalHub.send(recipientId, SIGNAL, new NotificationDto(
        notification.id,
        notification.kind,
        notification.text,
        actorId,
        actor == null ? null : actor.displayName,
        actor == null ? null : actor.avatarUrl,
        eventId,
        notification.createdAt,
        false));
  }

  /** @return true if it was the caller's and unread */
  @Transactional
  public boolean markRead(Long me, Long notificationId) {
    return notificationRepository.markRead(notificationId, me, Instant.now()) > 0;
  }

  /** @return how many were marked */
  @Transactional
  public int markAllRead(Long me) {
    return notificationRepository.markAllRead(me, Instant.now());
  }

  /** What the person reads. One line per kind: a new kind without a line does not compile. */
  private static String sentence(NotificationKind kind, String actor, String title) {
    return switch (kind) {
      case FRIEND_REQUEST -> actor + " wants to be your friend";
      case FRIEND_ACCEPTED -> actor + " accepted your friend request";
      case EVENT_INVITATION -> actor + " invited you to " + title;
      case INVITATION_ACCEPTED -> actor + " is coming to " + title;
      case INVITATION_DECLINED -> actor + " can't make it to " + title;
      case GUEST_JOINED -> actor + " joined " + title;
      case GUEST_LEFT -> actor + " left " + title;
      case REMOVED_FROM_EVENT -> "You were removed from " + title;
      case EVENT_CANCELLED -> actor + " cancelled " + title;
    };
  }
}
