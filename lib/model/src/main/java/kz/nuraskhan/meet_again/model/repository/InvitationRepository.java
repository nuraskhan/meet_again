package kz.nuraskhan.meet_again.model.repository;

import java.util.Optional;
import kz.nuraskhan.meet_again.model.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

  Optional<Invitation> findByEventIdAndInviteeUserAccountId(Long eventId, Long inviteeUserAccountId);
}
