package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
