# PLAN 006: expired events, checked every minute

The request is in `01-prompt-event-expiry.txt`.

## Statuses of an event

| Status | Meaning | Who sets it |
|--------|---------|-------------|
| `ACTIVE` | Open: people can be invited, join by the link, answer | Creation |
| `CANCELLED` | Called off | The creator |
| `EXPIRED` | Its last proposed date has passed | The timer; never a person |

`ACTIVE` and `CANCELLED` existed. `EXPIRED` is new. Both ends are final: nothing brings an event
back to `ACTIVE`.

## When an event expires

| Question | Decision |
|----------|----------|
| Which date counts | The LAST proposed date. While a later date is ahead, the plan is still on. |
| How soon after it | Three hours. A meeting takes time: a plan for 6 pm must not read "expired" at 6:01. |
| An event without dates | Never expires: there is nothing to pass. It stays active until its creator cancels it. |
| A cancelled event | Stays cancelled. |

## What is closed on an expired event

Joining by the link, accepting, declining and cancelling are refused with the code
`event_expired`. Reading stays: the event, its public page and its chat. An unanswered invitation
to it stops showing as waiting. Reminders are not sent (they go only for active events).

## The cron

`EventExpiryJob`, cron `0 * * * * *` - second 0 of every minute. One `UPDATE` marks every active
event that is over, so several servers running it at once cannot mark one twice.

It keeps no pointer to where it stopped: each run asks "which active events are over by now". A
server that was asleep (a Heroku Eco dyno sleeps after 30 minutes without requests, and a sleeping
server runs no timers) catches up on its first run after waking - within a minute of the first
request. Until then such an event still reads `ACTIVE`.

The switch for the timers is renamed from `magain.reminder.enabled` to `magain.timers.enabled`,
since it now covers two jobs.

## Schema: migration `V006`

The check constraint of `event.status` allows `EXPIRED`; an index on `event.status`.
