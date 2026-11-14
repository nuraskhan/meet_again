package kz.nuraskhan.hangelo.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One {@link Place} suggested for an {@link Event}. The rows of an event are computed together and
 * kept; asking again replaces all of them.
 */
@Entity
@Table(name = "event_suggestion")
public class EventSuggestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "event_id", nullable = false)
  public Long eventId;

  @Column(name = "place_id", nullable = false)
  public Long placeId;

  /** 1 = the best. */
  @Column(name = "rank", nullable = false)
  public Integer rank;

  /** Miles from the people whose location was known, at the moment of computing. */
  @Column(name = "average_distance_mi", nullable = false)
  public Double averageDistanceMi;

  @Column(name = "farthest_distance_mi", nullable = false)
  public Double farthestDistanceMi;

  /** Every one of those people is within their own travel radius of the place. */
  @Column(name = "within_everyones_reach", nullable = false)
  public boolean withinEveryonesReach;

  /** The prices of the place overlap the budget of the event. */
  @Column(name = "fits_budget", nullable = false)
  public boolean fitsBudget;

  /** How many people's locations the computing was based on. */
  @Column(name = "participants_considered", nullable = false)
  public Integer participantsConsidered;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
