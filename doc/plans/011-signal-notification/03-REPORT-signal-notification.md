# REPORT 011: signals from the server and the notification service

Built as described in `02-PLAN-signal-notification.md`.

## Backend

- Migration `V011__notification.sql`; jOOQ classes regenerated (`notification.kind` as an enum).
- `lib/model`: `NotificationKind`, `Notification`, `NotificationRepository`, `RND.notification`,
  `InvitationRepository.findGuestIds`.
- New module `magain-mod/notification`: `SignalHub`, `SignalController`, `NotificationService`,
  `NotificationQuery`, `NotificationController`, `NotificationDto`.
- `FriendService`, `EventService` call `NotificationService.tell`; `ChatController` signals a new
  message to the other members.
- `SecurityConfig`: the internal ASYNC dispatch of a stream that ends is let through (the first
  pass was checked; the second carries no token).
- Found while testing and fixed: open streams held the shutdown of the server for 30 seconds (it
  waits for requests in flight). `SignalHub` now ends its streams first when the server stops -
  this matters for every restart and deploy.

## Front end (repository `meet_again_ui`)

- `src/stores/signal.ts`, `src/stores/notifications.ts`, `openStream` in `src/api/http.ts`,
  notification calls and types in `src/api`.
- Bell with the unread number in `AppShell`; page `NotificationsView` at `/notifications`.
- `ChatView` shows signalled messages at once, polls every 30 s instead of 4 s as a safety net.
- `PlansView`, `PlanView`, `FriendsView` reread on a notification that concerns them.
- `server.js` closes the backend connection when the browser leaves.

## What was checked

- `gradlew testTz`: 0 failures in both time zones. New: `NotificationApiTest` (9 tests: every
  trigger and its sentence, who is not told, read / read-all / the count, paging, somebody else's
  is "not found", no token is 401), `SignalApiTest` (5 tests over a really open stream: a
  notification and a chat message arrive, the author gets none, every open page of the person gets
  it and nobody else, a failed action signals nothing, no token is 401), `RndPersistenceTest`
  extended.
- `npm run build` (type check + build) passes.
- By hand, backend + the UI's production server (`node server.js`), so through the Node proxy:
  `curl` on `/api/signal/stream` got `hello`, then the notification the moment a friend request
  was sent, and a `:ping` within 23 seconds. In Chrome: the bell went from 1 to 2 and the toast
  appeared without a reload; the notifications page listed both; "Mark all as read" cleared the
  bell. No console errors.

## Not done, and known limits

- Not checked on Heroku: nothing is deployed. The keep-alive (20 s) is under the router's 55 s
  limit, and the stream is renewed by the page when the router or the server ends it.
- The keep-alive runs on the server's timers, so `magain.timers.enabled=false` turns it off too
  (that is how the tests run).
- An Eco dyno that sleeps drops the streams; the page reconnects when the dyno wakes.
- One server process only: see "Known limit" in the plan.
- No notification for a new chat message (only the live signal): no unread counter per chat yet.
- No e-mail or browser push for notifications; a person sees them when the application is open.
- No Playwright test (`ui/itest` does not exist yet).

## For the human

- Review, then commit both repositories; push and deploy when decided. Migration V010 (the feed)
  and V011 are both not yet on Heroku.
