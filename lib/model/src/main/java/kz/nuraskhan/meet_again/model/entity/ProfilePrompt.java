package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One prompt of a {@link Profile}: a question the person chose ("My perfect weekend is...") and
 * their answer. Kept in the person's order.
 */
@Entity
@Table(name = "profile_prompt")
public class ProfilePrompt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  @Column(name = "position", nullable = false)
  public Integer position;

  @Column(name = "question", nullable = false)
  public String question;

  @Column(name = "answer", nullable = false)
  public String answer;
}
