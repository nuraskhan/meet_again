package kz.nuraskhan.meet_again.model.repository;

import kz.nuraskhan.meet_again.model.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {
}
