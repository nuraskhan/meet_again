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
import kz.nuraskhan.hangelo.model.enums.EventType;

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

  /** How good the place is, 0 to 5; null = nobody has rated it. */
  @Column(name = "rating")
  public Double rating;

  /**
   * Expected spending per person, in the same units as the budget of an event; both null = not
   * known (the place is then never left out for the budget).
   */
  @Column(name = "price_min")
  public Integer priceMin;

  @Column(name = "price_max")
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

  /** The city it is in: "Astana", "Almaty". Null for places put in before cities were kept. */
  @Column(name = "city")
  public String city;

  /** Where the record came from ("2GIS"); null = entered by hand. */
  @Column(name = "source")
  public String source;

  /** Its id at the source: loading the same place again updates this row instead of adding one. */
  @Column(name = "external_id")
  public String externalId;

  /** How many reviews the rating rests on; null = unknown. */
  @Column(name = "review_count")
  public Integer reviewCount;
}
