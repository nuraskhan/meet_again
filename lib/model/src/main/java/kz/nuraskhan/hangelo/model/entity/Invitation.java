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
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;

/** One invited person of an {@link Event} and their answer. */
@Entity
@Table(name = "invitation")
public class Invitation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "event_id", nullable = false)
  public Long eventId;

  @Column(name = "invitee__user_account_id", nullable = false)
  public Long inviteeUserAccountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  public InvitationStatus status;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "responded_at")
  public Instant respondedAt;

  /**
   * Filled = the person said "coming" and then left while the event was still on. Cleared if they
   * come back. Counts against their reliability.
   */
  @Column(name = "left_at")
  public Instant leftAt;

  /**
   * The creator marked, after the event, that this guest said "coming" and did not come. Counts
   * against the guest's reliability.
   */
  @Column(name = "no_show", nullable = false)
  public boolean noShow;
}
