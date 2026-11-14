# PLAN 008: a fuller profile, several photos, privilege levels

The request is in `01-prompt-profile.txt`.

## More about the person

| Field | Notes |
|-------|-------|
| Pronouns, lives in, from, what you do, education, languages | Free text, all optional. |
| A few words (bio) | As before. |
| Interests | Up to 10 short labels (30 characters), in the person's order; a repeat is kept once whatever the letter case. |
| Prompts | Up to 3: a question (picked from a list in the UI or written) and an answer. |
| Date of birth | **Now private.** The person sees it; everyone else sees only the age. Before, the date itself was shown to any logged-in user. |

A profile update replaces the whole: what is left out becomes empty.

## Several photos

| Question | Decision |
|----------|----------|
| How many | Decided by the privilege level: 3, 6 or 9. |
| The main one | The first. It is the picture shown next to the name everywhere (`profile.avatar_url` is its address). |
| What a person can do | Add (goes last), remove (the rest close the gap), make main (goes first, the others keep their order). |
| Size | Up to 6 MB to upload; kept at up to 1080 pixels on a side. |
| Turned photos | A phone stores a portrait photo sideways with a note to turn it. The note (EXIF orientation) is read, the pixels are turned for real, and the note is dropped with the rest of EXIF. |
| Safety | Everything from plan 004 holds: type by content, size and pixel limits, re-encoding, random addresses, nothing trusted from the upload. A large picture is now decoded already reduced, so it never sits in memory at full size. |
| The old single picture | Becomes the person's first photo (migration). The table `avatar` is gone. |

## Privilege levels

`profile.privilege`: `STANDARD` (everyone starts here), `PRO`, `MAX`. The base of the future
subscription plans.

- What a level allows is written in the enum `Privilege`, next to the level: today `maxPhotos`
  (3 / 6 / 9). Code asks the level for the allowance and never compares levels by name; a new
  allowance is a new field there.
- **Nobody can change their own level.** It is not among the fields a profile update accepts, and
  there is no endpoint that sets it. Until payments exist it is changed in the database.
- It is shown: to the person as their plan, to others as a "Pro" / "Max" mark.

## What others see

`GET /profile/{userId}`: everything above except the e-mail and the date of birth. A new page in
the UI shows it the way such apps do - photos on top with a bar per photo, name and age, the facts,
interests, prompts - in the current look of the site.

Search results return a short card (name, city, picture), not the whole profile.

## Schema: migration `V008`

`profile` gains `privilege`, `occupation`, `education`, `hometown`, `pronouns`, `languages`;
new tables `profile_interest`, `profile_prompt`, `profile_photo`; `avatar` is copied into
`profile_photo` and dropped. The order of a person's photos is unique, checked at the end of the
transaction, so photos can swap places inside one.

## API changes

- `POST /profile/me`: new fields, `interests`, `prompts`.
- `GET /profile/me`: adds `privilege`, `maxPhotos`, `photos`, `interests`, `prompts`, `age`.
- `GET /profile/{userId}`: `age` instead of `birthDate`; the new fields.
- `GET /profile/search`: a short card.
- New: `POST /profile/me/photo`, `POST /profile/me/photo/{id}/remove`,
  `POST /profile/me/photo/{id}/make-main`, `GET /public/photo/{token}`.
- Removed: `POST /profile/me/avatar`, `.../avatar/remove`, `GET /public/avatar/{token}`.
