package kz.nuraskhan.hangelo.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Who can log in. Everything a person tells about themselves is in {@link Profile}. */
@Entity
@Table(name = "user_account")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  /** Always in lower case. */
  @Column(name = "email", nullable = false)
  public String email;

  @Column(name = "password_hash", nullable = false)
  public String passwordHash;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  /** When the person confirmed that the address is theirs. Empty = not confirmed. */
  @Column(name = "email_verified_at")
  public Instant emailVerifiedAt;

  /** Sees the analytics page. Set by hand in the database, never by the application. */
  @Column(name = "admin", nullable = false)
  public boolean admin;

  /** Filled = cannot log in. */
  @Column(name = "deactivated_at")
  public Instant deactivatedAt;
}
