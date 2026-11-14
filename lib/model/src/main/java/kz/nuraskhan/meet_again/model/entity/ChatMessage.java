package kz.nuraskhan.meet_again.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_message")
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "chat_id", nullable = false)
  public Long chatId;

  @Column(name = "author__user_account_id", nullable = false)
  public Long authorUserAccountId;

  @Column(name = "text", nullable = false)
  public String text;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
