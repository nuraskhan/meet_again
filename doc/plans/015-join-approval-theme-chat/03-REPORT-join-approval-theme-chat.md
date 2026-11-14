# REPORT 015: join approval, accent colour, theme switch, the chat's way to its plan

Done as planned, in both repositories. Nothing is committed, pushed or deployed.

## Backend

- `V012__join_request.sql`; `InvitationStatus.REQUESTED`; `NotificationKind.JOIN_REQUESTED`,
  `JOIN_APPROVED`, `JOIN_DECLINED` and their sentences.
- `EventService`: `joinPublic` asks instead of joining; `approveRequest`, `declineRequest`;
  `leave` takes back a request. `EventController`: the two new endpoints; `join` answers
  `{standing}`.
- `EventQuery`: someone who only asked does not see the event; requests are in the people list
  for the creator only; a request is not counted as an invitation on the public page.
- `ChatDto` and `ChatQuery`: `eventId`, `eventType`.

## Front end

- Feed: "Ask to join"; after asking - "Request sent" with "Take back"; after a decline - "Ask
  again".
- Plan page, for the organiser: a block "Ask to join" with Approve and Decline per person.
- Notifications: the three new kinds, with icons; a declined request leads to the feed.
- Accent tokens; `stores/theme.ts`, `ThemeToggle`, the script in `index.html`; the map's dimming
  follows the chosen theme, not only the system.
- Chat: the title opens "About this chat" - a card to the plan and the members; the chat list
  shows the activity's icon.
- `design/`: the accent and the theme switch in the README, the prompt and the design system.

## What was checked

- `gradlew testTz`: 0 failures in both time zones. The feed tests were rewritten for asking and
  nine were added: asking leaves the stranger out (no chat, no event, the feed says "requested");
  asking twice is one request and one notification; someone invited joins at once; approve;
  decline and asking again; only the creator answers and only a waiting request; requests are
  seen by the creator only and give no say in the event; taking a request back; the three
  notifications; joining by the link still tells the creator. The chat list carries the event.
- `npm run build` passes.
- In Chrome against a local server: the brown accent in both themes; the switch turned a dark
  page light on a dark system; a request shown to the organiser, approved, the person moved into
  "People" and a toast said so; the chat's title opened the panel with the plan card and three
  members. No console errors.

## Not checked

- The feed as the person who asks ("Request sent", "Take back") - covered by backend tests, not
  seen in the browser.
- The switch on a phone and on the welcome page; the phone layouts in general are still unseen.

## Consequences to know

- Public plans that people joined before stay as they are: only new joins need approval.
- A person who is declined may ask again without limit; the creator's tool against that is not
  built (removing works only on a guest).
- Requests still waiting when a plan is cancelled or passes are not answered or announced.
- The log-in page has no theme switch (the welcome page and the application have).

## For the human

- Review; commit both repositories; deploy when decided (migration V012 runs on release).
