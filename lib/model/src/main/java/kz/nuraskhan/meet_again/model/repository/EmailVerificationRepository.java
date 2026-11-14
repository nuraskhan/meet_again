package kz.nuraskhan.meet_again.model.repository;

import java.util.Optional;
import kz.nuraskhan.meet_again.model.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

  Optional<EmailVerification> findByTokenHash(String tokenHash);

  /** The link sent to this account most recently. */
  Optional<EmailVerification> findFirstByUserAccountIdOrderByCreatedAtDesc(Long userAccountId);
}
