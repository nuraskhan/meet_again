package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** One per {@link UserAccount}, with the same id. */
@Entity
@Table(name = "profile")
public class Profile {

  @Id
  @Column(name = "user_account_id")
  public Long userAccountId;

  @Column(name = "display_name", nullable = false)
  public String displayName;

  @Column(name = "bio")
  public String bio;

  @Column(name = "city")
  public String city;

  @Column(name = "birth_date")
  public LocalDate birthDate;

  @Column(name = "avatar_url")
  public String avatarUrl;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
