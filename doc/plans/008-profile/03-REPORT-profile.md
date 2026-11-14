# REPORT 008: a fuller profile, several photos, privilege levels

Built as described in `02-PLAN-profile.md`.

## Backend

- Migration `V008__profile_details_photos_privilege.sql`; jOOQ classes regenerated
  (`profile.privilege` is read as the enum).
- `lib/model`: enum `Privilege`; `Profile` with the new fields; `ProfilePhoto`, `ProfileInterest`,
  `ProfilePrompt` with repositories and RND factories; `Avatar` removed.
- `profile` module, rewritten: `PhotoService` / `PhotoController` / `PublicPhotoController` in place
  of the avatar classes; `ImageSanitizer` (1080 pixels, reduced decoding, EXIF orientation);
  `ProfileService` (details, interests, prompts); `ProfileQuery` (age, lists); new DTOs.
- `application.yaml`: upload limit 6 MB.

## Front end (repository `meet_again_ui`)

- The profile page: a grid of photos with "Main", "Make main", "Remove", "Add photo" and the count
  against the plan; the new fields; interests as removable tags; up to three prompts with a list of
  questions to pick from; the plan shown; "See how others see it".
- A new page for a person (`/people/<id>`): photos with a bar per photo and previous / next, name
  and age, facts, interests, prompts.
- Names on the friends page and among the guests of a plan lead to that page.

## What was checked

- `gradlew testTz`: 454 test executions, 0 failures (227 tests in two time zones). New or
  rewritten: `PhotoApiTest` (18 tests), `ProfileApiTest` (10). Among them: three photos on
  STANDARD and the fourth refused, six on PRO and the seventh refused; make-main reorders and
  changes the picture in a friend's list; removing closes the gap and frees a place; only one's own
  photos; a photo stored sideways comes out upright (checked by the colour of its halves); a JPEG
  with a nonsense EXIF block is accepted and the block ignored; every upload attack from plan 004
  still refused; the privilege and a photo address sent in a profile update are ignored; others get
  the age and never the date of birth or the e-mail; the whole profile is replaced on save.
- The local database with an existing picture migrated: it became the first photo.
- In a browser, locally: the editor with two photos, details, four interests and two prompts; the
  person page with the gallery, "Alice, 30", facts, interests and prompts.

## Not checked

- Uploading through the browser's file picker in this round (done through the API); the same
  control was checked in plan 004.
- A real phone photo: the rotation is tested with a built file, not one from a camera.
- The phone layout.

## Known limits

- The privilege level can only be changed in the database: there are no payments and no admin
  screen yet.
- Lowering a level does not remove photos over the new limit; it only stops adding.
- Photos cannot be dragged into an order: only "make main".
- Photos are served from the database (see plan 004).
- Interests are free text: "Hiking" and "hikes" are different labels.
- HEIC photos are not accepted.

## Not done by the agent

Not committed, not deployed.
