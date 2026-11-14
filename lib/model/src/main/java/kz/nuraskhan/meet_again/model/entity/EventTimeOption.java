package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One of the dates the creator of an {@link Event} proposes. An event without any = "to be agreed". */
@Entity
@Table(name = "event_time_option")
public class EventTimeOption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "event_id", nullable = false)
  public Long eventId;

  @Column(name = "starts_at", nullable = false)
  public Instant startsAt;
}
