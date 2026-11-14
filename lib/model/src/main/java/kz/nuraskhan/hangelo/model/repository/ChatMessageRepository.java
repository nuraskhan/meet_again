package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  /** The id of the newest message of a chat; null for a chat nobody has written in. */
  @org.springframework.data.jpa.repository.Query(
      "select max(m.id) from ChatMessage m where m.chatId = :chatId")
  Long findNewestId(@org.springframework.data.repository.query.Param("chatId") Long chatId);
}
