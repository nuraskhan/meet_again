# PLAN 013: rebrand to Hangelo

## The rename, by rule

| Was | Becomes |
|-----|---------|
| `Meet Again`, `meet again` (what people read) | `Hangelo`, `hangelo` |
| Java package `kz.nuraskhan.meet_again.magain.<module>` | `kz.nuraskhan.hangelo.<module>` |
| Java package `kz.nuraskhan.meet_again.<lib>` | `kz.nuraskhan.hangelo.<lib>` |
| `MagainApplication` | `HangeloApplication` |
| Modules `applications/magain/magain-launcher`, `magain-mod` | `applications/hangelo/hangelo-launcher`, `hangelo-mod` |
| Settings prefix `magain.` and variables `MAGAIN_*` | `hangelo.` and `HANGELO_*` |
| Local databases `magain`, `magain_test`, user `magain` | `hangelo`, `hangelo_test`, user `hangelo` |
| Compose project `meet_again-local` | `hangelo-local` |
| Front end: package name, page title, wordmark, storage keys `meet_again.*` | `hangelo_ui`, `Hangelo`, `hangelo`, `hangelo.*` |
| `design/` documents | The new name throughout |

The two package levels `meet_again.magain` collapse into one: `hangelo.hangelo` would say nothing.

## What is not renamed, and why

- **Migrations**: applied files are never edited (they do not contain the name anyway).
- **`doc/plans/001`-`012`**: the record of what was asked and done at the time.
- **Outside the repositories** - the folders on disk, the GitHub repositories, the Heroku apps and
  their config vars, the token variables `MEET_AGAIN_KEY` and others: these belong to the human;
  `CLAUDE.md` keeps naming them as they are until they change.

## The local database

Renamed in place, nothing deleted: a new superuser `hangelo`, then `alter database … rename`.
The old role `magain` stays in an existing data directory as the owner of the objects; a fresh
`make recreate` gives a directory with `hangelo` only.

## Logo

`design/logo/` in the front-end repository: the sketch, and `LOGO-PROMPT.md` - what the sketch
fixes, what to clean up, the colours (the H in the earth brown of a footpath, the pin red), the
prompt itself with six variants, and how to judge the result. The design system gets a "Brand
mark" section reserving the two logo colours for the logo only.
