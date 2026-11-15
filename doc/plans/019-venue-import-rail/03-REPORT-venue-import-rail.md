# REPORT 019: load the venues, and no scrolling inside the feed's right panel

Nothing is committed, pushed or deployed.

## The import

- The first run was refused: a demo key takes at most 10 venues per request, not the 50 of the
  documentation. `PlaceImport` now reads page by page: 3 pages of 10 per category and city, and
  keeps the 20 with the most reviews. A refusal's reason is now read from the body of the answer.
- Local database: loaded. 334 venues (356 found; a venue matching two categories stays in the
  first, so some categories hold fewer than 20 - bars in Almaty 11, cinemas in Astana 15). Every
  one has a rating and a review count; 15 have no street address in the directory and carry the
  city instead. Names and addresses are in Russian, as the directory keeps them.
- **Live database: NOT loaded.** The first attempt failed in the TLS handshake with the database;
  the second, meant to find out why, was stopped by the session's permission check (direct
  access to the production database). It is the human's decision how to proceed.

## The right panel

- `FeedView`: the rail lost `position: sticky`, its `max-height` and `overflow-y: auto`.
- Checked in Chrome with a day chosen and the "Mark days" form open: the rail is 1109 px tall in a
  730 px window, nothing inside it scrolls, the page scrolls as a whole; no sideways scrolling.

## What was checked

- `:place-import:test` passes; `npm run build` passes.
- The loaded data was looked at by category and city, and a sample of rows read.

## For the human

- Decide how the live database gets its venues: allow the import to connect to it from this
  machine, or run the tool another way (for example as a one-off process on Heroku, which needs
  the 2GIS key there).
- Whether the key's plan allows keeping 2GIS data in our database is still to be checked.
- Review; commit; deploy when decided.
