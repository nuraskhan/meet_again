package kz.nuraskhan.hangelo.event.reminder;

import java.time.Duration;
import java.time.Instant;
import kz.nuraskhan.hangelo.mail.EmailSender;
import kz.nuraskhan.hangelo.mail.MailProperties;
import kz.nuraskhan.hangelo.event.reminder.ReminderQuery.DueEvent;
import kz.nuraskhan.hangelo.event.reminder.ReminderQuery.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reminds people by e-mail about a plan whose next proposed date is less than a day away.
 *
 * <p>Who gets it: the creator and the guests who said yes, if their address is confirmed. Once per
 * person per plan.
 *
 * <p>WHY it runs inside the server and what that costs: there is no separate worker yet, so the
 * job is a timer in this process. A server that is asleep runs no timers - on a Heroku Eco dyno,
 * which sleeps after 30 minutes without requests, reminders go out only while someone is using the
 * site, or late, when it wakes. A dyno that does not sleep (Basic), or an outside scheduler waking
 * the server, removes that.
 */
@Component
public class EventReminderJob {

  /** How long before the date the reminder goes out. */
  static final Duration AHEAD = Duration.ofHours(24);

  private static final Logger log = LoggerFactory.getLogger(EventReminderJob.class);

  private final ReminderQuery reminderQuery;
  private final ReminderLog reminderLog;
  private final EmailSender emailSender;
  private final MailProperties mailProperties;

  EventReminderJob(ReminderQuery reminderQuery, ReminderLog reminderLog, EmailSender emailSender,
                   MailProperties mailProperties) {
    this.reminderQuery = reminderQuery;
    this.reminderLog = reminderLog;
    this.emailSender = emailSender;
    this.mailProperties = mailProperties;
  }

  /** The timer: a minute after start, then ten minutes after each run ends. */
  @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT10M")
  void onTimer() {
    try {
      int sent = sendDue(Instant.now());
      if (sent > 0) {
        log.info("Reminders sent: {}", sent);
      }
    } catch (RuntimeException e) {
      // A failed run must not stop the timer: the next one tries again.
      log.error("The reminder run failed", e);
    }
  }

  /**
   * Sends every reminder that is due at the given moment.
   *
   * @return how many letters went out
   */
  public int sendDue(Instant now) {
    int sent = 0;

    for (DueEvent event : reminderQuery.dueEvents(now, now.plus(AHEAD))) {
      for (Recipient recipient : reminderQuery.recipients(event)) {
        if (remind(event, recipient, now)) {
          sent++;
        }
      }
    }

    return sent;
  }

  private boolean remind(DueEvent event, Recipient recipient, Instant now) {
    try {
      reminderLog.claim(event.eventId(), recipient.userId(), now);
    } catch (DataIntegrityViolationException e) {
      return false; // another server took this one a moment ago
    }

    try {
      emailSender.send(ReminderLetter.build(
          recipient.email(), recipient.displayName(), event.title(), event.startsAt(),
          event.timeZone(), event.optionsAhead(), mailProperties.link("/plans/" + event.eventId())));
      return true;
    } catch (RuntimeException e) {
      reminderLog.release(event.eventId(), recipient.userId());
      log.warn("The reminder about event {} for user {} was not sent: {}",
          event.eventId(), recipient.userId(), e.getMessage());
      return false;
    }
  }
}
