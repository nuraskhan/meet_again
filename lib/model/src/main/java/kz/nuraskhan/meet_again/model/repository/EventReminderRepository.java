package kz.nuraskhan.meet_again.model.repository;

import kz.nuraskhan.meet_again.model.entity.EventReminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventReminderRepository extends JpaRepository<EventReminder, Long> {

  void deleteByEventIdAndUserAccountId(Long eventId, Long userAccountId);
}
