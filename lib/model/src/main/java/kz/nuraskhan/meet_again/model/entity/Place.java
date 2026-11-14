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
import kz.nuraskhan.meet_again.model.enums.EventType;

/** A place people can meet at. Suggested for events of the same category that it is near to. */
@Entity
@Table(name = "place")
public class Place {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "name", nullable = false)
  public String name;

  /** What one does there: the same values as the type of an event. */
  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  public EventType category;

  @Column(name = "description")
  public String description;

  @Column(name = "address", nullable = false)
  public String address;

  @Column(name = "latitude", nullable = false)
  public Double latitude;

  @Column(name = "longitude", nullable = false)
  public Double longitude;

  /** How good the place is, 0 to 5. */
  @Column(name = "rating", nullable = false)
  public Double rating;

  /** Expected spending per person, in the same units as the budget of an event. */
  @Column(name = "price_min", nullable = false)
  public Integer priceMin;

  @Column(name = "price_max", nullable = false)
  public Integer priceMax;

  @Column(name = "website")
  public String website;

  @Column(name = "phone")
  public String phone;

  /** Invented for demonstration, not a real business. */
  @Column(name = "sample", nullable = false)
  public boolean sample;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  /** Filled = no longer suggested. */
  @Column(name = "deactivated_at")
  public Instant deactivatedAt;
}
