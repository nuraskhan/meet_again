# REPORT 007: suggesting places from where everyone is coming from

Built as described in `02-PLAN-place-suggestion.md`.

## Backend

- Migration `V007__place_and_suggestion.sql`; jOOQ classes regenerated (`place.category` is read as
  the `EventType` enum).
- `lib/model`: `Place`, `EventSuggestion`, `Event.suggestedAt`, with repositories and RND factories.
- `event` module: `SuggestionService` (who counts, which places, the order, saving), `Geo`
  (distance), `SuggestionDto`; `EventQuery` reads the saved suggestions and works out the caller's
  own distance; `EventService.savePreference` accepts the creator and triggers the first suggestion.
- Launcher: `SamplePlaces` and a second, separately checked part of `DemoData`.

## Front end (repository `meet_again_ui`)

- The plan page: the location form is shown to the creator too; a block "Where to go" with the
  button and up to three places.

## What was checked

- `gradlew testTz`: 434 test executions, 0 failures (217 tests in two time zones). New:
  `SuggestionApiTest` (12 tests), `GeoTest` (6). Among them: the right category, deactivated and
  far places left out; best rated first; a place inside the budget before a better rated one
  outside it; a place everyone can reach before a better rated stretch; nothing found is
  remembered; the type OTHER takes any category; suggestions stay as saved until asked again;
  they appear by themselves when everyone coming is on the map and do not change when someone
  moves; a typed place alone is not enough; only participants of an active event may ask; nobody's
  coordinates are in the answer; New York to Los Angeles is about 2446 miles.
- Locally with the sample places: the creator pinned in Park Slope and a guest who said yes in
  Williamsburg - after the second pin three bathhouses appeared by themselves, the nearest first
  by rating, 2.9 miles apart as expected. In a browser: the block, the three cards with rating,
  distances, prices and the "Sample" mark; "Suggest again".

## Known limits

- **Production has no places.** Until the table is filled, "Suggest places" answers that nothing
  was found.
- Distances are straight lines, not travel time.
- A participant who only typed a place is not taken into account.
- The search box does not wrap around the date line or the poles.
- Ratings are a number in our table; nobody can rate a place yet.
- Suggestions do not refresh when people join, leave or move - only on the button.

## Not done by the agent

Not committed, not deployed.
