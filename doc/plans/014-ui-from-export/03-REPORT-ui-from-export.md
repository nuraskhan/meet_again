# REPORT 014: the front end rebuilt on the designers' export

Done as planned, in the repository `meet_again_ui`. Nothing is committed, pushed or deployed.

## What changed

- The stylesheet, the frame, the logo and favicon, icons instead of emoji (new dependency
  `lucide-vue-next`).
- All 13 pages and the shared components (`EventSummary`, `ProposedTimes`, `PreferenceForm`,
  `LocationMap`, `SuggestedPlaces`).
- New behaviour that came with the design:
  - chats are two panes on a wide screen (the `chat` route is now a child of `chats`; addresses
    are the same), with day dividers, runs of messages and a "New messages" pill;
  - a live preview of the invitation beside the new-plan form;
  - friend actions on a person's page;
  - a saved location on a plan folds into one line with "Change";
  - notifications grouped by day, with an icon of the kind;
  - "Save" on the profile is enabled only after a change; a "show password" button;
  - past plans fold behind a row.

## What was checked

- `npm run build` (type check and build): passes.
- No old class, old colour variable or emoji is left in `src`.
- In Chrome, wide screen, dark theme, against a local server with prepared people, plans, a chat
  and requests: welcome, log in, plans, feed, a plan, chats with a chat open, friends,
  notifications, my profile, a person, new plan. All render in the new design; no console errors.

## Not checked

- The phone and tablet layouts: the browser window could not be made narrow in this session.
  They are written, not seen.
- The light theme (the machine is in dark mode).
- Pages and states not opened: the invitation by link, e-mail confirmation, empty and error
  states, the new-plan form after choosing an activity, uploads, suggested places with data.
- There are no end-to-end tests yet; when they exist, note that some texts and a few elements
  changed (below).

## Left out: the export shows it, the server does not give the data

| Where | What | Needs |
|-------|------|-------|
| Invitation by link | Faces of people coming; the inviter's picture | The going list and the creator's avatar on the public event |
| Plans, a plan | Pictures in avatars (initials are shown) | Avatar addresses of the creator and the invitees in the event |
| A plan | The organiser's location as guests see it | The creator's preference in the event |
| Chats | The activity icon per chat; "Open the plan" in a chat | The event type and event id in the chat |
| Chat | A message shown before the server confirms it, "Try again" | Client work judged not safe to add in passing |
| Profile | Upload progress in percent (the tile shows as busy) | Progress events in the HTTP layer |

## Decisions made on the way, worth a look

- The feed card's photo is the inviter's profile picture: a plan has no photo of its own.
- "Plans" keeps a "New plan" button in its head although the sidebar has one.
- Real limits are shown where the export's sample text differs (6 MB photos, 1000 characters).
- Texts changed to the designers' wording on many buttons and badges ("I'm coming", "I can't",
  "Coming", "Not answered", "Cancel the plan", "Save").
- Elements that changed kind: the private/public choice is two buttons with `aria-checked`
  (were radio inputs); a notification row is a link (was a button); `plan-closed` is a badge;
  `chat-message` is on each bubble; a saved location must be opened with `preference-change`
  before editing.
- The welcome and log-in pages carry the same decorative picture twice in code; it should become
  one component.
- Small date helpers are repeated in four views; they belong in `src/lib/format.ts`.

## For the human

- Look at it on a phone and in the light theme.
- `proto/` is not committed and not ignored: decide whether the export lives in the repository.
- Review; commit; deploy when decided (the Heroku config vars of report 013 first).
