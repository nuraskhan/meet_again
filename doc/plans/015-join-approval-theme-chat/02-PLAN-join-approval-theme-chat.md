# PLAN 015: join approval, accent colour, theme switch, the chat's way to its plan

## 1. A public plan is joined by asking

- A new standing of a person in an event, `REQUESTED`: asked from the feed, the creator has not
  answered. Not a guest: not in the chat, does not see the event, takes no place.
- `POST /event/{id}/join` creates the request and answers `{standing}`; it no longer returns the
  event (the person does not see it yet).
- `POST /event/{id}/request/{userId}/approve` and `/decline`, creator only. Approve: a guest who
  is coming, in the chat. Decline: may ask again.
- `POST /event/{id}/leave` also takes back a waiting request.
- Unchanged: someone invited by name joins at once (asking is their yes); the link joins at once
  (the creator handed it out); a removed person cannot ask.
- The requests are in the event's people list for the creator only.
- Told: the creator when someone asks (`JOIN_REQUESTED`); the one who asked when answered
  (`JOIN_APPROVED`, `JOIN_DECLINED`).
- Migration `V012__join_request.sql`: the two check constraints get the new values.

## 2. Accent: the earth brown of the logo, not blue

The accent tokens are replaced in the application's own part of the stylesheet; the designers'
part stays as they sent it. The design documents follow.

## 3. Theme switch

A switch between light and dark: at the foot of the sidebar on wide screens (they have no top
bar), in the top bar on a phone, and on the welcome page. Until it is pressed the system setting
decides; after, the choice is remembered in the browser. The theme is `data-theme` on `<html>`,
set before the first paint.

## 4. Chat: its plan and its people

- The chat list and the chat know the event (`eventId`, `eventType` in the chat).
- The title of an open chat is a button: it opens a panel with a card that leads to the plan and
  the list of members, each leading to their profile.
- The list of chats shows the activity's icon.
