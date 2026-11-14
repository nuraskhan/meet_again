# REPORT 004: profile picture as an uploaded file

Built as described in `02-PLAN-avatar-upload.md`.

## Backend

- Migration `V004__avatar_file.sql`; jOOQ classes regenerated.
- `lib/model`: `Avatar` with its repository and `RND.avatar`.
- `profile` module: `ImageSanitizer` (recognise, check, decode, scale to 512, encode again),
  `AvatarService`, `AvatarController`, `PublicAvatarController`; `UpdateProfileRequest` lost
  `avatarUrl`.
- `lib/web`: an upload over the limit is answered with 413 and the usual error body.
- `application.yaml`: upload limits.

## Front end (repository `meet_again_ui`)

- The profile page: "Upload picture" / "Change picture" / "Remove" instead of the link field, with
  a check of type and size before sending and the server's refusal shown in place.
- `UserAvatar` loads a picture only from a `/public/...` path of our server.
- The map on the guest's location form (OpenStreetMap, Leaflet) with a pin that can be placed and
  dragged - done just before this task, UI only.

## What was checked

- `gradlew testTz`: 332 test executions, 0 failures (166 tests in two time zones). New:
  `AvatarApiTest` (13 tests). Refused: HTML, SVG, a Windows program, a zip archive and a GIF under
  picture names and types; a path as the file name; files with a picture signature and garbage
  after it; a cut-off picture; a 45-byte PNG declaring 30000 x 30000 pixels; an empty file; a
  2.5 MB file; an upload without a token; setting a link through the profile update. Accepted and
  verified: a PNG and a JPEG, scaling 2000 x 1000 down to 512 x 256, transparency kept as PNG,
  a script glued after a valid picture not present in what is served, the old address dead after a
  new upload and after removal, the security headers of the served picture.
- In a browser, locally: an HTML file named `.png` was refused with the server's message; a real
  PNG was uploaded and appeared in the profile and in the top bar.

## Known limits

- No virus scanning. It matters little here because the uploaded bytes are never kept or served.
- No limit on how often a user uploads.
- Pictures are served from the database: fine for an MVP, to be moved to object storage when there
  are many users.
- JPEGs in the CMYK colour space (rare, from print workflows) are refused as unreadable.
- WebP, GIF and HEIC are not accepted. A photo straight from an iPhone camera may be HEIC; browsers
  usually convert it to JPEG when it is picked from the photo library.

## Not done by the agent

Not committed, not deployed.
