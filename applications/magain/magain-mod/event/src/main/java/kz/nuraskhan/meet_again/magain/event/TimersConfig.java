package kz.nuraskhan.meet_again.magain.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns the timers of the server on: the reminders (EventReminderJob, every ten minutes) and the
 * expiry of events (EventExpiryJob, every minute).
 *
 * <p>{@code magain.timers.enabled=false} turns them off. The tests do that and call the jobs
 * themselves, for the moment they choose.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "magain.timers.enabled", havingValue = "true", matchIfMissing = true)
class TimersConfig {
}
