package kz.nuraskhan.meet_again.model.repository;

import java.util.Optional;
import kz.nuraskhan.meet_again.model.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  /** @param email in lower case */
  Optional<UserAccount> findByEmail(String email);

  /** @param email in lower case */
  boolean existsByEmail(String email);
}
