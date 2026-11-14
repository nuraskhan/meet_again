package kz.nuraskhan.meet_again.magain.event.dto;

import java.time.Instant;
import java.util.List;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import kz.nuraskhan.meet_again.model.enums.EventType;

/**
 * A public event as the feed shows it to any logged-in person: what the invitation says, who
 * invites, who is coming - and where the reader stands.
 *
 * <p>Not here: the chat (one joins to enter it), the link of the event, and anything guests said
 * about their location.
 *
 * @param status      always ACTIVE: the feed shows nothing else
 * @param goingCount  how many guests are coming (the creator is not counted)
 * @param going       the first few of them, to show faces
 * @param full        no place is left
 * @param myStanding  CREATOR, or the reader's own answer (PENDING, ACCEPTED, DECLINED, REMOVED), or
 *                    NONE for someone who has nothing to do with the event yet
 */
public record FeedItemDto(
    Long id,
    EventType eventType,
    String title,
    String description,
    Integer distanceMinMi,
    Integer distanceMaxMi,
    Integer priceMin,
    Integer priceMax,
    List<Instant> proposedTimes,
    EventStatus status,
    Instant createdAt,
    Long creatorId,
    String creatorName,
    String creatorAvatarUrl,
    String creatorCity,
    int goingCount,
    List<Person> going,
    boolean full,
    String myStanding
) {

  /** Someone in the feed: enough to show a face and to open their profile. */
  public record Person(Long userId, String displayName, String avatarUrl) {
  }
}
