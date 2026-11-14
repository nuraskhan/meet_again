# PLAN 011: signals from the server and the notification service

## What is asked

The backend tells the front end that something happened, without the page asking; and there is a
notification service that does the telling.

## Two things, kept apart

| Thing | What it is | Where it lives |
|-------|------------|----------------|
| Signal | A hint pushed to the pages a person has open, right now. Not stored, not retried. | `SignalHub`, memory of the server |
| Notification | A record: "Aida invited you to Lagman on Friday". Read later, marked read. | table `notification` |

A notification is written to the database and then signalled. A chat message is only signalled (the
message itself is the record). Losing a signal loses nothing: the page reads the database on load
and again after a break in the connection.

## Transport: Server-Sent Events

One request that stays open, `GET /signal/stream`, into which the server writes events.

- Server to page only, which is all that is needed; the page speaks with ordinary requests.
- Plain HTTP: passes the Vite proxy, the Node proxy of the UI and the Heroku router unchanged, uses
  the same bearer token as every other request. No broker, no new dependency.
- The page reads it with `fetch`, not `EventSource`: `EventSource` cannot send the token in a
  header, and a token in the address ends up in logs.
- WebSocket/STOMP was not taken: two-way, needs its own authentication and proxy handling, and
  nothing here needs the page to push.

Events: `hello` (the stream is open), `notification` (a NotificationDto), `chat.message`
(`{chatId, message}`); a `:ping` comment every 20 seconds keeps the connection alive (the Heroku
router cuts one that is silent for 55 seconds).

## Backend

- Migration `V011__notification.sql`: table `notification` (recipient, kind, actor, event, the
  sentence, created, read).
- `lib/model`: `NotificationKind`, `Notification`, `NotificationRepository`, `RND.notification`.
- New module `magain-mod/notification`:
  - `SignalHub` - keeps the open streams per person (at most 5, each lives 30 minutes), `send`
    goes out after the transaction commits, closes the streams when the server stops.
  - `SignalController` - `GET /signal/stream`.
  - `NotificationService.tell(recipient, kind, actor, eventId)` - composes the sentence, saves,
    signals. Telling someone about their own action does nothing.
  - `NotificationController` - `GET /notification/list`, `GET /notification/unread-count`,
    `POST /notification/{id}/read`, `POST /notification/read-all`.
- Who is told what:

| Happens | Who is told | Kind |
|---------|-------------|------|
| Friend request sent | the addressee | FRIEND_REQUEST |
| Friend request accepted | the requester | FRIEND_ACCEPTED |
| Event created with invitees | each invitee | EVENT_INVITATION |
| Invitation accepted / declined | the creator | INVITATION_ACCEPTED / INVITATION_DECLINED |
| Guest joins (public or by link) | the creator | GUEST_JOINED |
| Guest leaves | the creator | GUEST_LEFT |
| Guest removed | the guest | REMOVED_FROM_EVENT |
| Event cancelled | guests invited or coming | EVENT_CANCELLED |
| Chat message posted | the other members | signal `chat.message` only |

A declined friend request is not announced.

## Front end (`meet_again_ui`)

- `stores/signal.ts`: opens the stream at login, closes at logout, reconnects (1 s doubling to
  30 s), emits `reconnected` so listeners reread.
- `stores/notifications.ts`: unread count, list, toast for a new one.
- Bell with the unread number in the top bar; page `/notifications` (open the thing, mark read,
  mark all read).
- Chat shows a message the moment it is written; polling stays as a slow safety net (30 s).
- Plans, a plan, and Friends reread on a notification that concerns them.
- `server.js`: closes the backend connection when the browser leaves a stream.

## Known limit

Streams are in the memory of one server process. With a second server, `SignalHub.send` must go
through something both hear (PostgreSQL LISTEN/NOTIFY or a broker); nothing else changes.
