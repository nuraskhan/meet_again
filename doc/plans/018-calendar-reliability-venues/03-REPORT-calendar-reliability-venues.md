# REPORT 018: calendar of availability, reliability rating, venues, default pictures

Built as planned, in both repositories, except request 5: the venues are NOT loaded - there is no
2GIS key yet. Nothing is committed, pushed or deployed.

## What changed

- Backend: migrations V015, V016; new modules `hangelo-mod/calendar` and `hangelo-mod/reliability`;
  new tool `place-import`; in `event` - attaching a venue, "did not come", `PlaceController`;
  profiles and the feed carry the reliability.
- Front end: `CalendarPanel`, `CalendarView`, `ReliabilityBadge`, `EventPicture`, `PlacePicker`,
  `src/lib/dates.ts`; the feed's right rail; the plan page (venue, who came, picture); plan cards,
  the invitation page, a person's page, the profile, the new-plan form; "Calendar" in the sidebar.

## What was checked

- `gradlew testTz`: 612 test executions in two time zones, 0 failures. New: `AvailabilityApiTest`
  (8), `ReliabilityApiTest` (7), `VenueApiTest` (6), `TwoGisParserTest` (4), and the availability
  entity in `RndPersistenceTest`.
- `npm run build` passes.
- In Chrome against a local server: the feed with the rail (the reliability card, the calendar
  with busy, free and trip bands, a plan's dot, friends' marks), the pictures on the cards; the
  plan page - the picker, choosing a venue, the venue card. At 390 px: the feed, the calendar
  page, the plan page; none scrolls sideways.

## Not checked

- **The import from 2GIS was never run.** Only the reading of an answer is tested, on samples
  written from the documentation. The real answer may differ; whether a key sees review counts
  depends on its plan.
- In the browser: adding and removing a mark in the calendar, the "Air" sticker (no test person
  had a score), "did not come", the new-plan busy hints, a person's coming days, the dark theme
  of the new screens.

## Decisions made on the way

- Unknown prices and ratings are empty, not made-up numbers; a venue with unknown prices is never
  left out for the budget.
- The mark "did not come" is seen by the creator only; the guest sees its effect in their rating.
- A venue found under two categories keeps the first.
- The feed card's large picture is the drawn scene now, not the inviter's photo.

## Known limits and open questions

- Storing data of the 2GIS API in our database: whether the key's plan allows it must be checked
  by the owner of the key. The documentation read here does not say.
- The rating can be gamed: a creator can mark anyone absent, with no appeal.
- A person sees their own rating fall but not which event caused it.
- Three sample venues were added to the LOCAL database for the check (marked "sample").
- The design documents in `design/` do not describe the new screens yet.

## For the human

- Put a 2GIS key into the user environment variable `HANGELO_2GIS_KEY`, then `make places` (local)
  or the task `:place-import:importTo` with `HANGELO_IMPORT_DB_*` for the live database.
- Review; commit both repositories; deploy when decided (V015 and V016 run on release).
