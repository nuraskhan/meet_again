package kz.nuraskhan.meet_again.model.repository;

import java.util.Optional;
import kz.nuraskhan.meet_again.model.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

  Optional<Invitation> findByEventIdAndInviteeUserAccountId(Long eventId, Long inviteeUserAccountId);

  /** How many guests an event has now: invited and not yet answered, or coming. */
  @Query("""
      select count(i) from Invitation i
      where i.eventId = :eventId
        and i.status in (kz.nuraskhan.meet_again.model.enums.InvitationStatus.PENDING,
                         kz.nuraskhan.meet_again.model.enums.InvitationStatus.ACCEPTED)
      """)
  long countGuests(@Param("eventId") Long eventId);
}
