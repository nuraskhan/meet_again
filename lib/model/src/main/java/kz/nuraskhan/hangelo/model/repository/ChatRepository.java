package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {
}
