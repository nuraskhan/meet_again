# REPORT 021: venues that can actually be seen

Done as planned, in both repositories. Nothing is committed, pushed or deployed - and so the live
site still has no venues.

## What changed

- Backend: `SuggestionService` looks wider when nothing is near; `PlaceSearchRequest` and
  `POST /place/search`; `PlaceDto.distanceMi`; `EventQuery.searchPlaces` ordered by distance from
  a point.
- Release: `place-import` is staged for Heroku (`:place-import:stage`, the root `stage`); the
  `Procfile` runs it after the migrations when `HANGELO_2GIS_KEY` is set; `PlaceImport` skips when
  venues from 2GIS are already there (`HANGELO_IMPORT_ONLY_IF_EMPTY`) and takes the user and the
  password out of the address before connecting.
- Front end: `PlacePicker` rows show the distance beside the rating, "Nearest first", and a
  button to measure from the device; the plan page gives the picker the saved location; a
  suggestion card no longer says the same distance twice.

## What was checked

- `gradlew testTz`: 0 failures in both time zones. New: looking wider and not looking wider when
  something is near (`SuggestionApiTest`); nearest first with distances, and refusing half a
  position (`VenueApiTest`); the address without credentials (`PlaceImportTest`).
- `npm run build` passes.
- Locally, with the 334 real venues: a person in the centre of Astana with a radius of 1 mile
  got three suggestions with rating and distance; the search returned the nearest first.
- In Chrome: suggestion cards (name, rating, distance); the picker (name, rating, distance,
  "Nearest first").
- The release step run by hand against the local database, with an address that carries the user
  and the password as Heroku's does: "already there: nothing to do", exit 0.

## Not checked

- The release step on Heroku itself, and the first real load there.
- The button "Show how far each is" (the browser's permission prompt cannot be answered here).
- The wider look in the browser (the tests cover it).

## For the human

- To load the venues on the live site: set the config var `HANGELO_2GIS_KEY` on the Heroku
  application `meet-again-api` (Settings, Config Vars), then deploy. The next release loads them;
  later releases skip the step.
- The 2GIS key will then be stored on Heroku. Whether the key's plan allows keeping 2GIS data in
  our database is still for the owner of the key to check.
- Review; commit; deploy when decided.
