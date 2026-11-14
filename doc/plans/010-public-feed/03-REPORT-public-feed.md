# REPORT 010: public invitations and the feed

Built as described in `02-PLAN-public-feed.md`.

## Backend

- Migration `V010__public_events.sql`; jOOQ classes regenerated (`event.visibility` as an enum).
- `lib/model`: `EventVisibility`; `InvitationStatus.REMOVED` and `isIn()`; `Event.visibility`;
  `InvitationRepository.countGuests`.
- `event` module: `FeedQuery`, `FeedController`, `FeedItemDto`; in `EventService` - `joinPublic`,
  one shared `join` (also used by the link), `leave`, `removeGuest`, the limit `MAX_GUESTS`; a
  removed guest no longer sees the event (`EventQuery.visibleTo`) and has no say in it.

## Front end (repository `meet_again_ui`)

- A new page "Feed", first in the navigation: cards with the inviter (leading to their profile),
  the plan, the note, faces of those coming, and "Join"; a filter by activity; "Show older".
- The invitation form: "Only people I invite" or "Everyone".
- The plan page: a "Public" mark; the inviter's name leads to their profile; "I can't come after
  all" for a guest; a remove control per guest for the creator; both with a confirmation.

## What was checked

- `gradlew testTz`: 486 test executions, 0 failures (243 tests in two time zones). New:
  `FeedApiTest` (15 tests) - a public event is in the feed with the inviter, a private one never
  is (and private is the default); cancelled and expired ones drop out; the filter and the paging;
  joining makes a guest and opens the chat for writing; joining twice; a private event by its id
  and an unknown one are "not found"; a full event refuses the 51st guest and shows five faces;
  leaving and coming back; removal - out of the chat and the list, refused from the feed and by
  the link, no say in preferences or suggestions; only the creator removes, and only a guest.
- An older test that failed once in this round (`SuggestionApiTest`) was made independent of what
  earlier runs left in the test database: it now switches off whatever is around its spot first.
- In a browser, locally: the feed with two public plans, the inviter and the faces of two guests;
  "Join" led to the plan page with the "Public" mark, the chat button and "I can't come after
  all".

## Not checked

- In a browser: the form's "Everyone" option, leaving, and removing a guest (all three are covered
  by the API tests only); the phone layout.

## Known limits

- See "Not in this change" in the plan: no reporting, blocking, moderation, or posting limits.
- The feed is one for the whole world: no "near me", no search.
- Visibility cannot be changed after creation.
- A removed guest's messages stay in the chat.

## Not done by the agent

Not committed, not deployed.
