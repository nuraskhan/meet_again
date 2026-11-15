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
import java.time.LocalDate;
import kz.nuraskhan.hangelo.model.enums.AvailabilityKind;

/**
 * Days a person marked in their calendar: busy, free, or away in another city.
 *
 * <p>Whole days, as the person means them. A date has no time zone: "the 5th" is the 5th for
 * everyone who looks, wherever the server and the reader are.
 */
@Entity
@Table(name = "availability")
public class Availability {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false)
  public AvailabilityKind kind;

  @Column(name = "from_date", nullable = false)
  public LocalDate fromDate;

  /** The last day, included. */
  @Column(name = "to_date", nullable = false)
  public LocalDate toDate;

  /** Where to, for a TRIP. */
  @Column(name = "city")
  public String city;

  /** For the person alone: friends never see it. */
  @Column(name = "note")
  public String note;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  /** Filled = removed by the person. */
  @Column(name = "deleted_at")
  public Instant deletedAt;
}
