package kz.nuraskhan.hangelo.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A letter sent to a {@link UserAccount} to confirm its e-mail address: it carries a link and a
 * six-digit code, and either of them confirms.
 */
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

  /**
   * SHA-256 of the six-digit code in the letter, in hex. Null for letters sent before codes
   * existed: those are confirmed by their link only.
   */
  @Column(name = "code_hash")
  public String codeHash;

  /** How many wrong codes were typed against this letter; after a few it accepts no more. */
  @Column(name = "failed_attempts", nullable = false)
  public int failedAttempts;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  public Instant expiresAt;

  /** Filled = the link or the code was used; neither works again. */
  @Column(name = "used_at")
  public Instant usedAt;
}
