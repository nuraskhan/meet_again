package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The profile picture of a {@link UserAccount}, one per account, with the same id.
 *
 * <p>The content is never what the user uploaded: it is the image the server decoded and encoded
 * again (see AvatarService in the profile module).
 */
@Entity
@Table(name = "avatar")
public class Avatar {

  @Id
  @Column(name = "user_account_id")
  public Long userAccountId;

  /** The secret part of the address of the picture. New on every upload. */
  @Column(name = "token", nullable = false)
  public String token;

  /** {@code image/jpeg} or {@code image/png}, nothing else. */
  @Column(name = "content_type", nullable = false)
  public String contentType;

  @Column(name = "content", nullable = false)
  public byte[] content;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
