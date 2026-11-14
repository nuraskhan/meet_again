package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One interest of a {@link Profile}: a short label, "hiking". Kept in the person's order. */
@Entity
@Table(name = "profile_interest")
public class ProfileInterest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  @Column(name = "position", nullable = false)
  public Integer position;

  @Column(name = "label", nullable = false)
  public String label;
}
