# REPORT 020: plans as lines in the calendar; photographs as default pictures

Done as planned, in the front-end repository. Nothing is committed, pushed or deployed.

## What changed

- `CalendarPanel`: `calendar__plans` under the date - a line per plan (up to three), or the count
  on a tint; the colour by activity (`PLAN_COLOUR`). The legend's "Plan" is a line now.
- `EventPicture`: shows a photograph; if the file fails to load, the icon of the activity.
- `src/assets/event/`: nine photographs (960 x 540 JPEG, 55 to 172 KB each, 835 KB in all) and
  `CREDITS.md` with the page of each and the licence.

## What was checked

- `npm run build` passes; the nine files are in the build.
- Every photograph was looked at before use. One was replaced: the first for "Eat together"
  showed alcohol brands.
- In Chrome, wide screen, light theme: the feed's cards with photographs; the calendar with one
  line (a day with one plan), two lines (two plans), and "4" on a tint (four plans).

## Not checked

- A day with exactly three plans; the dark theme; the phone width; the plan page, plan cards and
  the invitation page with the photographs (they use the same component).

## To know

- The photographers are not named in `CREDITS.md`: their names could not be read automatically.
  The licence does not require credit; the pages are listed.
- "Something else" is a night street of a foreign city; a picture closer to home may suit better.
- A plan's colour is by its activity, so two plans of one activity on a day are two lines of one
  colour.
- The drawn scenes are gone from the code.

## For the human

- Review; commit; deploy when decided.
