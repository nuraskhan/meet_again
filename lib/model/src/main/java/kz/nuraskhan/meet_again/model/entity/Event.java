package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import kz.nuraskhan.meet_again.model.enums.EventType;

/**
 * The invitation form as its creator filled it. The people invited are {@link Invitation}s, the
 * proposed dates are {@link EventTimeOption}s.
 */
@Entity
@Table(name = "event")
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "creator__user_account_id", nullable = false)
  public Long creatorUserAccountId;

  /** The group chat created together with the event. */
  @Column(name = "chat_id", nullable = false)
  public Long chatId;

  /**
   * The secret part of the link to the event. Whoever has it can see the event without logging in
   * and, once logged in, join it.
   */
  @Column(name = "share_token", nullable = false)
  public String shareToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  public EventType eventType;

  @Column(name = "title", nullable = false)
  public String title;

  @Column(name = "description")
  public String description;

  /** Preferred distance from the creator, miles. */
  @Column(name = "distance_min_mi", nullable = false)
  public Integer distanceMinMi;

  @Column(name = "distance_max_mi", nullable = false)
  public Integer distanceMaxMi;

  /** Expected spending per person. */
  @Column(name = "price_min", nullable = false)
  public Integer priceMin;

  @Column(name = "price_max", nullable = false)
  public Integer priceMax;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  public EventStatus status;

  /**
   * The time zone the proposed dates are meant in (IANA name), from the creator's browser. Empty
   * for events made before it was recorded.
   */
  @Column(name = "time_zone")
  public String timeZone;

  /**
   * When places were last suggested for the event. Empty = never; filled while there are no
   * {@link EventSuggestion}s = looked for, and nothing suitable was found.
   */
  @Column(name = "suggested_at")
  public Instant suggestedAt;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
