package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * What a guest of an {@link Event} says about themselves for that event: where they are and how
 * far they are ready to travel.
 *
 * <p>The place is coordinates (from the device, with the person's permission), a text the person
 * typed, or both; never neither.
 */
@Entity
@Table(name = "participant_preference")
public class ParticipantPreference {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "event_id", nullable = false)
  public Long eventId;

  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  /** Filled together with {@link #longitude}, or both empty. */
  @Column(name = "latitude")
  public Double latitude;

  @Column(name = "longitude")
  public Double longitude;

  /** The place as the person typed it: a city, a neighbourhood, an address. */
  @Column(name = "location_text")
  public String locationText;

  /** The farthest the person is ready to go, miles. */
  @Column(name = "travel_radius_mi", nullable = false)
  public Integer travelRadiusMi;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
