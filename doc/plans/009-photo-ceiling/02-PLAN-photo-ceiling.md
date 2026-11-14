# PLAN 009: at most 10 photos per profile

The request is in `01-prompt-photo-ceiling.txt`.

**How it was read.** "Max 10 photos per profile" is taken as a ceiling no profile can pass, whatever
its privilege level - not as "everyone gets 10". The levels from plan 008 stay: STANDARD 3, PRO 6.
The highest level, MAX, goes from 9 to 10, so the ceiling is reachable.

| Level | Photos before | Photos now |
|-------|---------------|------------|
| STANDARD | 3 | 3 |
| PRO | 6 | 6 |
| MAX | 9 | 10 |

## Where the limit lives

1. `Privilege.PHOTO_CEILING = 10`. A level declared with more than that fails at startup, so a
   future level cannot promise what a profile cannot hold.
2. The database (migration `V009`): `profile_photo.position <= 10`. Positions run 1, 2, 3...
   without gaps, so position 11 is an eleventh photo. This holds even for a write that bypasses the
   server's check.

Nothing changes in the API or the UI: the UI shows whatever `maxPhotos` the server reports.
