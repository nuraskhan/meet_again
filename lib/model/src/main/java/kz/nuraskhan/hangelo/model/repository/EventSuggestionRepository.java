package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.EventSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventSuggestionRepository extends JpaRepository<EventSuggestion, Long> {

  /**
   * Removes the suggestions of an event at once, in the database, before new ones are written
   * (the new rows reuse the same ranks, which are unique per event).
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from EventSuggestion s where s.eventId = :eventId")
  void deleteAllOfEvent(@Param("eventId") Long eventId);
}
