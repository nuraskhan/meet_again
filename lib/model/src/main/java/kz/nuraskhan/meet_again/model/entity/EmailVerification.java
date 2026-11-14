package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A link sent to a {@link UserAccount} to confirm its e-mail address. */
@Entity
@Table(name = "email_verification")
public class EmailVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  /** SHA-256 of the token in the link, in hex. The token itself is never stored. */
  @Column(name = "token_hash", nullable = false)
  public String tokenHash;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  public Instant expiresAt;

  /** Filled = the link was used and does not work again. */
  @Column(name = "used_at")
  public Instant usedAt;
}
