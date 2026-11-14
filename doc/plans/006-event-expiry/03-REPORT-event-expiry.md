# REPORT 006: expired events, checked every minute

Built as described in `02-PLAN-event-expiry.md`.

## Backend

- Migration `V006__event_status_expired.sql`; jOOQ classes regenerated (the new index).
- `lib/model`: `EventStatus.EXPIRED`.
- `event` module: `expiry/EventExpiryJob` (the cron); `EventService.requireActive` - the one place
  that refuses a change to a closed event and says why; `TimersConfig` replaces
  `reminder/ReminderSchedulingConfig`.
- `application.yaml`: `magain.timers.enabled` instead of `magain.reminder.enabled`.

## Front end (repository `meet_again_ui`)

- The list of plans is split: what is on, and "Past and cancelled" below it, dimmed.
- A past plan is labelled "Past"; its page and its public invitation page say "This plan is over".

## What was checked

- `gradlew testTz`: 398 test executions, 0 failures (199 tests in two time zones). New:
  `EventExpiryApiTest` (6 tests) - expires after the only date; not within three hours of it; waits
  for the last of several dates; never an undated or a cancelled event; reports how many it marked
  and marks each once; an expired event refuses joining, accepting, declining and cancelling, and is
  still readable.
- The cron in a running server, locally: a plan dated five hours in the past was created and was
  `EXPIRED` at the next full minute, with nothing else touching it.

## Known limits

- An event without dates never expires.
- The three hours are one number for every kind of event.
- A proposed date in the past is accepted when a plan is created; such a plan expires at the next
  run.
- On a sleeping server the status changes when it wakes (see the plan).
- Nobody is told that a plan expired.

## Not done by the agent

Not committed, not deployed.
