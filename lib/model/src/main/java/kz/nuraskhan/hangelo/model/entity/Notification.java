package kz.nuraskhan.hangelo.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import kz.nuraskhan.hangelo.model.enums.NotificationKind;

/** Something that happened and that one person should know about. */
@Entity
@Table(name = "notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  /** Whom it is for. */
  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false)
  public NotificationKind kind;

  /** Who did it; empty when nobody in particular did. */
  @Column(name = "actor__user_account_id")
  public Long actorUserAccountId;

  /** The event it is about; empty when it is not about an event. */
  @Column(name = "event_id")
  public Long eventId;

  /** The sentence shown to the person, as it read when the thing happened. */
  @Column(name = "text", nullable = false)
  public String text;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  /** Empty = not read yet. */
  @Column(name = "read_at")
  public Instant readAt;
}
