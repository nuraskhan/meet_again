package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.EventReminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventReminderRepository extends JpaRepository<EventReminder, Long> {

  void deleteByEventIdAndUserAccountId(Long eventId, Long userAccountId);
}
