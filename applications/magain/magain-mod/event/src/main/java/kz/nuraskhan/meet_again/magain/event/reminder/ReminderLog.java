package kz.nuraskhan.meet_again.magain.event.reminder;

import java.time.Instant;
import kz.nuraskhan.meet_again.model.entity.EventReminder;
import kz.nuraskhan.meet_again.model.repository.EventReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The record of reminders sent. Each method is its own short transaction, so a record is visible
 * to other instances of the server the moment it is made.
 */
@Service
class ReminderLog {

  private final EventReminderRepository eventReminderRepository;

  ReminderLog(EventReminderRepository eventReminderRepository) {
    this.eventReminderRepository = eventReminderRepository;
  }

  /**
   * Takes the right to remind this person about this event. Made BEFORE the letter is sent: if two
   * servers reach the same reminder at once, the unique pair in the table lets one through and
   * fails the other with DataIntegrityViolationException.
   */
  @Transactional
  public void claim(Long eventId, Long userAccountId, Instant now) {
    EventReminder reminder = new EventReminder();
    reminder.eventId = eventId;
    reminder.userAccountId = userAccountId;
    reminder.sentAt = now;
    eventReminderRepository.saveAndFlush(reminder);
  }

  /** The letter did not go out: give the right back, so the next run tries again. */
  @Transactional
  public void release(Long eventId, Long userAccountId) {
    eventReminderRepository.deleteByEventIdAndUserAccountId(eventId, userAccountId);
  }
}
