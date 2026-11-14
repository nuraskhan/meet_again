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

/** The invitation form as its creator filled it. The people invited are {@link Invitation}s. */
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

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  public EventType eventType;

  @Column(name = "title", nullable = false)
  public String title;

  @Column(name = "description")
  public String description;

  /** Preferred distance from the creator, kilometres. */
  @Column(name = "distance_min_km", nullable = false)
  public Integer distanceMinKm;

  @Column(name = "distance_max_km", nullable = false)
  public Integer distanceMaxKm;

  /** Expected spending per person. */
  @Column(name = "price_min", nullable = false)
  public Integer priceMin;

  @Column(name = "price_max", nullable = false)
  public Integer priceMax;

  /** When; empty = to be agreed in the chat. */
  @Column(name = "starts_at")
  public Instant startsAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  public EventStatus status;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
