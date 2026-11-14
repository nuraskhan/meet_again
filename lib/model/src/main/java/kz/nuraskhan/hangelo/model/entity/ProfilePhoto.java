package kz.nuraskhan.hangelo.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One photo of a {@link Profile}. A person has several, in their own order; the one at position 1
 * is the main photo.
 *
 * <p>The content is never what the user uploaded: it is the image the server decoded and encoded
 * again (see PhotoService in the profile module).
 */
@Entity
@Table(name = "profile_photo")
public class ProfilePhoto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  /** 1 = the main photo. Without gaps: 1, 2, 3... */
  @Column(name = "position", nullable = false)
  public Integer position;

  /** The secret part of the address of the photo. */
  @Column(name = "token", nullable = false)
  public String token;

  /** {@code image/jpeg} or {@code image/png}, nothing else. */
  @Column(name = "content_type", nullable = false)
  public String contentType;

  @Column(name = "content", nullable = false)
  public byte[] content;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
