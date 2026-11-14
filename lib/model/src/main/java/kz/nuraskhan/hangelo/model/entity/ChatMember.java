package kz.nuraskhan.hangelo.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_member")
public class ChatMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "chat_id", nullable = false)
  public Long chatId;

  @Column(name = "user_account_id", nullable = false)
  public Long userAccountId;

  @Column(name = "joined_at", nullable = false)
  public Instant joinedAt;

  /** Filled = no longer in the chat. */
  @Column(name = "left_at")
  public Instant leftAt;

  /**
   * The last message of the chat this member has seen; null = none. What others wrote after it is
   * unread for the member.
   */
  @Column(name = "last_read__chat_message_id")
  public Long lastReadChatMessageId;
}
