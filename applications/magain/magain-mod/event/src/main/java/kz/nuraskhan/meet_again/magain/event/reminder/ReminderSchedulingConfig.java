package kz.nuraskhan.meet_again.magain.event.reminder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns the timers on. {@code magain.reminder.enabled=false} turns them off: the tests do that and
 * call {@link EventReminderJob#sendDue} themselves, at the moment they choose.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "magain.reminder.enabled", havingValue = "true", matchIfMissing = true)
class ReminderSchedulingConfig {
}
