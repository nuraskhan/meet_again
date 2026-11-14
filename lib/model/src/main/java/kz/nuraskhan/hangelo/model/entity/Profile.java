package kz.nuraskhan.hangelo.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import kz.nuraskhan.hangelo.model.enums.Privilege;

/**
 * One per {@link UserAccount}, with the same id: what a person tells about themselves. Their
 * photos are {@link ProfilePhoto}s, their interests {@link ProfileInterest}s, their prompts
 * {@link ProfilePrompt}s.
 */
@Entity
@Table(name = "profile")
public class Profile {

  @Id
  @Column(name = "user_account_id")
  public Long userAccountId;

  /**
   * The privilege level: the base of the future subscription plans. Set by us, never by the person
   * - it is not among the fields a profile update accepts.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "privilege", nullable = false)
  public Privilege privilege = Privilege.STANDARD;

  @Column(name = "display_name", nullable = false)
  public String displayName;

  @Column(name = "bio")
  public String bio;

  /** Where the person lives now. */
  @Column(name = "city")
  public String city;

  /** Where the person is from. */
  @Column(name = "hometown")
  public String hometown;

  /** Known only to the person; others are shown the age. */
  @Column(name = "birth_date")
  public LocalDate birthDate;

  @Column(name = "pronouns")
  public String pronouns;

  /** What the person does: a job, a trade, "student". */
  @Column(name = "occupation")
  public String occupation;

  @Column(name = "education")
  public String education;

  /** The languages the person speaks, as they wrote them. */
  @Column(name = "languages")
  public String languages;

  /**
   * The address of the main photo (the {@link ProfilePhoto} at position 1), kept here so that
   * everything showing a person's picture reads one column. Null = no photos.
   */
  @Column(name = "avatar_url")
  public String avatarUrl;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
