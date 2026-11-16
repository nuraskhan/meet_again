# Plan: eleven fixes (confirmation required, chat replies, no demo login, admin and tag in the database, calendar, brainb.kz/hangelo)

## What is asked and what is decided

| # | Asked | Decision |
|---|-------|----------|
| 1 | Opening the notifications resets the count, and they stay read | The page marks everything read when it opens (and what arrives while it is open). For that visit the rows keep their "new" mark, so the person sees which ones they had not seen. |
| 2 | Answer a message in a chat | `chat_message.reply_to__chat_message_id`. An answer carries the beginning of the answered message (140 characters) and its author. Only a message of the same chat can be answered. |
| 3 | Remove the fast logins and the Steve accounts | The endpoint, the service, the setting and the pages are deleted. The two accounts are deleted from the database by the migration, with everything that belongs to them. |
| 4 | Nothing is allowed before the e-mail is confirmed | A filter after the token filter refuses every request of an unconfirmed account with `403 email_not_verified`, except `GET /profile/me` and `POST /account/email-verification/send`. The web application keeps such a token apart ("pending") and shows one page: "Check your inbox". |
| 5 | Admins are marked in the database | `user_account.admin`. The setting `hangelo.admin.emails` is removed. |
| 6 | Calendar "as Garmin does" | A ruled month; each plan is a chip in its day with the icon, a short time and the name, in the colour of the activity; marks are bands with their name. In a narrow month (phone, rail) a chip is a dot. |
| 7 | Air as an emoji | The badge shows the emoji and the percentage, no word. |
| 8 | A double tap zooms on a phone | `touch-action: manipulation` on the page; fields have 16px text on a phone (iOS zooms into smaller ones). Pinch zoom stays. |
| 9 | Move to brainb.kz/hangelo/ | The application is built and served under `/hangelo/`. brainb.kz has its own nginx: a block for it is written (`deploy/nginx-brainb.kz.conf` in the web repository); adding it there is the owner's step. |
| 10 | A tag by some accounts, set in the database only | `profile.tag` (1 to 24 characters). Shown by the name; not accepted by the profile update. |
| 11 | IntelliJ connection to the database | `.idea/dataSources.xml` with the two local databases. Production is not put into a committed file. |

## Why a filter for the confirmation, and not a check per endpoint

A new endpoint is closed by default. A check that has to be remembered will be forgotten once.

## Deleting the demonstration accounts

No table has `on delete cascade`, so the migration deletes in order: messages (moving the "read up
to here" marks of other chats back first), chat members, notifications, reminders, suggestions,
proposed times, preferences, invitations, the events and their chats, friendships, marks, the
profile, the account. Visits stay counted, without the account. Real people who joined a plan made
by a demonstration account lose that plan: it goes with its creator.

## What existing people will meet

- Accounts that never confirmed their e-mail are closed until they do: at the next request the web
  application takes them to "Check your inbox".
- Nobody is an administrator until marked in the database.
- Addresses from before the move (`/plans/7`, a link in a letter already sent) are redirected into
  `/hangelo/`.

## Checks

- `gradlew testTz`; `npm run build`.
- In the browser, against the local stack: every item above, and a 390 px wide page.
