# REPORT 009: at most 10 photos per profile

Built as described in `02-PLAN-photo-ceiling.md`.

- `lib/model`: `Privilege.PHOTO_CEILING`, the guard in the constructor, MAX = 10.
- Migration `V009__photo_ceiling.sql`; jOOQ classes regenerated.

## What was checked

- `gradlew testTz`: 458 test executions, 0 failures (229 tests in two time zones). New in
  `PhotoApiTest`: on the level MAX ten photos are accepted and the eleventh is refused with
  "10 photos"; no level allows more than ten.
- The database, directly: inserting a photo at position 11 is refused by the constraint
  `profile_photo__ceiling`.

## Not done by the agent

Not committed, not deployed.
