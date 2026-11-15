# REPORT 017: places when planning alone, a share button, the site icon, folding the sidebar, deleting, phone layouts

Done as planned, in both repositories. Nothing is committed, pushed or deployed.

## What changed

- Backend: `V014__event_deleted.sql`; `Event.deletedAt`; `EventService.suggest`, `delete`, and
  `leave` for a closed event; `ChatService.close`; `SuggestRequest`; `EventQuery` leaves out
  deleted events, and events a person declined once they are over.
- Front end: `SuggestedPlaces` (asks the device when nobody is on the map); the invite block and
  the delete buttons on the plan page; a note in "About this chat"; the fold button and its
  styles; `favicon.ico` removed; phone fixes.

## What was checked

- `gradlew testTz`: 0 failures in both time zones. New: three suggestion tests (one press when
  alone and the position kept; a chosen location is not moved; a stranger's position is refused
  and nothing saved) and `EventDeleteApiTest` (6 tests: gone for everyone with its chat and link;
  guests told when it was still on; out of the feed; a cancelled one too, and only once; creator
  only; a guest clearing a cancelled event from their lists without anyone being told).
- `npm run build` passes.
- In Chrome, wide screen, light theme: the plan page with "Share" and "Copy link", the delete
  button, the sidebar folded and unfolded.
- At 390 px (each page loaded in a frame of that width): plans, feed, a plan from top to bottom,
  chats, an open chat, friends, notifications, my profile, a person, new plan, welcome, create
  account, an invitation by link. No page scrolls sideways.

## Found at 390 px and fixed

- The welcome page's top bar did not fit: "Create account" was cut. The bar keeps "Log in" on a
  phone; the big button is right under it.
- A scrollbar under the feed's filters.
- The icon of the kind of a notification covered the initials on the avatar.
- "Something else" was cut on the activity tiles.
- A safeguard: one too-wide element can no longer make the whole page scroll sideways.

## Not checked

- "Suggest places" asking the device for its position: the browser's permission prompt cannot be
  answered in this session. The server side is covered by tests.
- Pressing "Share" and the delete buttons in the browser (the server side is covered by tests).
- The tablet width (768 to 1279) as such: its sidebar is the folded one, which was seen.
- A real phone: the frames show the layout, not touch, the keyboard or the safe areas.

## Known limits

- On the live site the table of places is empty, so "Suggest places" finds nothing there until
  places are loaded - the open question from report 007.
- Deleting a chat alone is not possible: a chat lives with its plan.
- A deleted plan cannot be restored from the application (the row stays in the database).
- A notification about a deleted plan leads to "not found".

## For the human

- Review; commit both repositories; deploy when decided (migration V014 runs on release).
