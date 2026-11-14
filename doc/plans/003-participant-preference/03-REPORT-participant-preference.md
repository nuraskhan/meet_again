# REPORT 003: participant preferences, US units

Built as described in `02-PLAN-participant-preference.md`.

## Backend

- Migration `V003__miles_and_participant_preference.sql`; jOOQ classes regenerated.
- `lib/model`: `ParticipantPreference` with its repository and `RND.participantPreference`;
  `Event.distanceMinMi` / `distanceMaxMi`; the defaults of `EventType` are in miles.
- `event` module: `EventService.savePreference`, `SavePreferenceRequest`, `PreferenceDto`;
  `InviteeDto` and `EventDto` extended; `EventQuery` reads preferences with the guests.

## Front end (repository `meet_again_ui`)

- `PreferenceForm`: "Use my current location" (the browser's permission prompt) or a typed place,
  and the travel radius as quick choices (1, 5, 10, 25, 50 mi) or any number of miles.
- The plan page shows the form to a guest, highlighted until it is filled, and shows next to each
  guest what the others may know ("Brooklyn, NY · up to 5 mi").
- Accepting from the list of plans now leads to the plan page.
- Every distance is shown in miles.

## What was checked

- `gradlew testTz`: 306 test executions, 0 failures (153 tests in two time zones). New:
  `PreferenceApiTest` (7 tests) - coordinates returned to the owner only, a typed place shown to the
  others, saving again replaces, kept per event, incomplete or impossible values refused, only a
  guest who has not declined may save, nothing on the public page.
- The local database with existing data migrated.
- In a browser, locally, as a guest: the highlighted form, typing a place, choosing 5 mi, saving;
  the guest row then read "Brooklyn, NY · up to 5 mi".

## Not checked

- The "Use my current location" button with a real permission prompt: the automated browser cannot
  answer the prompt. The code path is covered only by the API test that sends coordinates.

## Known limits

- A typed place is not turned into coordinates, so nothing can be computed from it yet.
- The preference is not required to accept.
- Rounding in the kilometre-to-mile conversion: 3 km became 2 mi, 15 km became 9 mi.

## Not done by the agent

Not committed, not deployed.
