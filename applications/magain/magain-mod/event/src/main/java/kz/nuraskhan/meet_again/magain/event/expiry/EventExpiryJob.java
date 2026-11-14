package kz.nuraskhan.meet_again.magain.event.expiry;

import static kz.nuraskhan.meet_again.jooq.Tables.EVENT;
import static kz.nuraskhan.meet_again.jooq.Tables.EVENT_TIME_OPTION;

import java.time.Duration;
import java.time.Instant;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every minute, marks as EXPIRED the active events whose dates are behind.
 *
 * <p>An event expires when its LAST proposed date is more than {@link #GRACE} in the past: while a
 * later date is still ahead, the plan is still on. An event without dates ("to be agreed in the
 * chat") has nothing to pass and stays active until its creator cancels it.
 *
 * <p>The job does not remember where it stopped: each run asks "which active events are over by
 * now". So a server that was asleep - and ran no timers - catches up on its first run after waking.
 */
@Component
public class EventExpiryJob {

  /**
   * How long after its last date an event still counts as going on. A meeting takes time: a plan
   * for 6 pm must not read "expired" at 6:01.
   */
  static final Duration GRACE = Duration.ofHours(3);

  private static final Logger log = LoggerFactory.getLogger(EventExpiryJob.class);

  private final DSLContext dsl;

  EventExpiryJob(DSLContext dsl) {
    this.dsl = dsl;
  }

  /** The cron: second 0 of every minute. */
  @Scheduled(cron = "0 * * * * *")
  void onTimer() {
    try {
      int expired = expireDue(Instant.now());
      if (expired > 0) {
        log.info("Events expired: {}", expired);
      }
    } catch (RuntimeException e) {
      // A failed run must not stop the timer: the next minute tries again.
      log.error("The expiry run failed", e);
    }
  }

  /**
   * Marks every event that is over at the given moment. One statement, so it is safe when several
   * servers run it at once: a row is updated by whichever gets there first, and only once.
   *
   * @return how many events were marked
   */
  @Transactional
  public int expireDue(Instant now) {
    Instant overBefore = now.minus(GRACE);

    return dsl
        .update(EVENT)
        .set(EVENT.STATUS, EventStatus.EXPIRED)
        .where(EVENT.STATUS.eq(EventStatus.ACTIVE))
        // It has dates...
        .andExists(DSL
            .selectOne()
            .from(EVENT_TIME_OPTION)
            .where(EVENT_TIME_OPTION.EVENT_ID.eq(EVENT.ID)))
        // ...and none of them is still ahead or going on.
        .andNotExists(DSL
            .selectOne()
            .from(EVENT_TIME_OPTION)
            .where(EVENT_TIME_OPTION.EVENT_ID.eq(EVENT.ID))
            .and(EVENT_TIME_OPTION.STARTS_AT.ge(overBefore)))
        .execute();
  }
}
