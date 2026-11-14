# REPORT 002: invite by link, several proposed dates

Built as described in `02-PLAN-invite-by-link.md`.

## Backend

- Migration `V002__event_link_and_time_options.sql`; jOOQ classes regenerated (`make jooq`).
- `lib/model`: `Event.shareToken` instead of `startsAt`; new `EventTimeOption` with its repository
  and `RND.eventTimeOption`.
- `event` module: `EventService.joinByLink`, `EventQuery.byShareToken`, `PublicEventController`,
  `PublicEventDto`; `CreateEventRequest` and `EventDto` changed.
- `chat` module: `ChatService.join` (a new member, or one who left and returns).
- `auth` module: `/public/**` is open without a token.

## Front end (repository `meet_again_ui`)

- New page `/i/<token>`: the invitation for anyone. A visitor sees "Register to accept" and
  "I already have an account"; after either they return to the invitation and press "I'm in".
- The invitation form: several dates ("Offer another date"); ticking friends is optional.
- The plan page: the proposed dates, and for the creator the link with a "Copy link" button.

## What was checked

- `gradlew testTz`: 292 test executions, 0 failures (146 tests in two time zones). New:
  `EventLinkApiTest` (10 tests) - read without a token, no guest names in the public answer,
  joining needs a token, a stranger joins, joining twice, joining after declining, the creator
  joining, a cancelled event, an unknown link.
- The local database with existing data migrated: the old demo event kept its date as its single
  proposed date.
- In a browser, locally: opened a link while logged out, registered a new user from it, came back
  to the invitation, pressed "I'm in", landed on the plan as a guest; as the creator saw the link
  block; the form with two date fields.

## Known limits

- No voting on the proposed dates.
- A link cannot be revoked or regenerated; cancelling the event is the only way to stop it.
- No limit on how many people join by one link.
- The public endpoint has no rate limiting.

## Not done by the agent

Not committed, not deployed. The live site on Heroku still runs the previous version; the backend
and the UI of this change have to go out together.
