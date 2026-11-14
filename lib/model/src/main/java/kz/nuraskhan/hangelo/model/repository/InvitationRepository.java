package kz.nuraskhan.hangelo.model.repository;

import java.util.Optional;
import kz.nuraskhan.hangelo.model.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

  Optional<Invitation> findByEventIdAndInviteeUserAccountId(Long eventId, Long inviteeUserAccountId);

  /** How many guests an event has now: invited and not yet answered, or coming. */
  @Query("""
      select count(i) from Invitation i
      where i.eventId = :eventId
        and i.status in (kz.nuraskhan.hangelo.model.enums.InvitationStatus.PENDING,
                         kz.nuraskhan.hangelo.model.enums.InvitationStatus.ACCEPTED)
      """)
  long countGuests(@Param("eventId") Long eventId);

  /** The user ids of the guests an event has now: invited and not yet answered, or coming. */
  @Query("""
      select i.inviteeUserAccountId from Invitation i
      where i.eventId = :eventId
        and i.status in (kz.nuraskhan.hangelo.model.enums.InvitationStatus.PENDING,
                         kz.nuraskhan.hangelo.model.enums.InvitationStatus.ACCEPTED)
      order by i.id
      """)
  java.util.List<Long> findGuestIds(@Param("eventId") Long eventId);
}
