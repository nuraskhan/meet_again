# REPORT 001: MVP backend

What was built for `01-prompt-mvp-backend.txt` following `02-PLAN-mvp-backend.md`.

## How to run

| Command | What happens |
|---------|--------------|
| `make start` | PostgreSQL, migrations, demo data, then the server on http://localhost:29000 |
| `make up` | The same without the server |
| `make test` | All tests against the test database |
| `make test-tz` | The same tests in Asia/Almaty and America/Los_Angeles |
| `make jooq` | Regenerates `lib/jooq` after a new migration |
| `make recreate` | Wipes all local data and builds the stack again |

Demo users: `alice`, `bob`, `carol`, `dan`, `eve` `@demo.test`, password `demo123`. Alice has three
friends, a pending request from Eve, and a bathhouse event with a chat.

The local database is `magain` on port 20000, user and password `magain`; the tests use
`magain_test` on the same server. Data is in `.data/` (git-ignored).

## API

Every request except `/auth/*` and `/actuator/health` needs the header
`Authorization: Bearer <token>`. A refusal has the body `{"code": "...", "message": "..."}`.
Moments are UTC with `Z`.

| Method and path | What it does |
|-----------------|--------------|
| `POST /auth/register` | Creates an account and a profile, returns a token |
| `POST /auth/login` | Returns a token |
| `GET /profile/me` | Own profile, with e-mail |
| `POST /profile/me` | Replaces the editable part of the own profile |
| `GET /profile/{userId}` | Another person's profile |
| `GET /profile/search?text=` | People by a part of the name or by the exact e-mail |
| `GET /friend/list` | Friends |
| `POST /friend/request` | Sends a friend request |
| `GET /friend/request/incoming` | Requests waiting for the caller's answer |
| `GET /friend/request/outgoing` | Requests the caller sent |
| `POST /friend/request/{requestId}/accept` | Accepts |
| `POST /friend/request/{requestId}/decline` | Declines |
| `POST /friend/{userId}/remove` | Ends the friendship |
| `GET /event/type/list` | Event types with the values the form is pre-filled with |
| `POST /event/create` | Creates an event, the invitations and the group chat |
| `GET /event/list` | Events the caller created or is invited to |
| `GET /event/{eventId}` | One event with its invitees and their answers |
| `POST /event/{eventId}/cancel` | Cancels; creator only |
| `GET /invitation/incoming` | Invitations waiting for the caller's answer |
| `POST /invitation/{invitationId}/accept` | Accepts |
| `POST /invitation/{invitationId}/decline` | Declines and leaves the chat of the event |
| `GET /chat/list` | Chats the caller is in |
| `GET /chat/{chatId}/member/list` | People in the chat |
| `GET /chat/{chatId}/message/list?beforeId=&limit=` | Messages, newest first, paged |
| `POST /chat/{chatId}/message` | Posts a message |

Event types: BATH, FOOD, COFFEE, BAR, WALK, CINEMA, SPORT, BOARD_GAMES, OTHER. Their defaults are
in `EventType`.

## What was checked

- `make test-tz`: 266 test executions, 0 failures - 32 API and persistence tests and 101 `RND` unit
  test executions, each in two time zones.
- `make up` on an empty data directory, then again: the second run changes nothing.
- The server started with `bootRun`; logged in as `alice@demo.test` with curl and read the profile,
  friends, incoming requests, events and chats; a request without a token got 401.

## Known limits

- Chat messages are stored and read over REST; nobody is notified. Live delivery needs the broker
  (RabbitMQ, ports 20001-20003), which is not started yet.
- Price is a whole number without a currency.
- "Preferred distance" is stored as the creator wrote it. There are no coordinates yet, so nothing
  is computed from it.
- A token cannot be revoked before it expires (30 days). There is no logout, password change or
  password reset.
- No rate limiting on registration and login.
- jOOQ 3.21 (open source) warns during `make jooq` that PostgreSQL 17 is older than the version it
  targets.
  Everything used here works; moving the image to PostgreSQL 18 removes the warning.
- Of the Makefile targets listed in `CLAUDE.md`, only the ones in "How to run" exist. Images,
  end-to-end tests, the dev-prod stack and the release targets come with the front end and CI.

## Not done by the agent

Commit and push are left to the human. The push to GitHub is still blocked: the token in
`MEET_AGAIN_KEY` has no write access to the repository.
