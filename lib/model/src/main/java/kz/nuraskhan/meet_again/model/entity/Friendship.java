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
import kz.nuraskhan.meet_again.model.enums.FriendshipStatus;

/** A friend request and what became of it. */
@Entity
@Table(name = "friendship")
public class Friendship {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "requester__user_account_id", nullable = false)
  public Long requesterUserAccountId;

  @Column(name = "addressee__user_account_id", nullable = false)
  public Long addresseeUserAccountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  public FriendshipStatus status;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "responded_at")
  public Instant respondedAt;
}
