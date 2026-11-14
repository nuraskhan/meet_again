package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A reminder about an {@link Event} sent to one person. At most one per person per event. */
@Entity
@Table(name = "event_reminder")
public class EventReminder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "event_id", nullable = false)
  public Long eventId;

  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  @Column(name = "sent_at", nullable = false)
  public Instant sentAt;
}
