package kz.nuraskhan.meet_again.model.repository;

import java.util.Optional;
import kz.nuraskhan.meet_again.model.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

  /** Also returns a member who has left: check {@code leftAt}. */
  Optional<ChatMember> findByChatIdAndUserAccountId(Long chatId, Long userAccountId);
}
