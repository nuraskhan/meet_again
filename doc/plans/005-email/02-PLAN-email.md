# PLAN 005: e-mail - confirming an address, reminders about upcoming events

The request is in `01-prompt-email.txt`.

## The provider: Resend

Resend, as suggested. It is one HTTP call per letter, has a free tier that covers an MVP, and needs
no library. The rest of the system does not know the provider: it sees the interface `EmailSender`
(module `lib/mail`), so another provider is one new class.

Without a key (`MAGAIN_RESEND_API_KEY`) letters are not sent: they are written to the log. That is
how a developer's machine works, and how the link of a confirmation letter is got locally.

## Confirming an address

| Question | Decision |
|----------|----------|
| When the letter goes out | At registration. If it cannot be sent, registration still succeeds; the letter can be asked for again from the site. |
| The link | `<site>/verify-email?token=...`, works 24 hours, once. Opening it needs no login. |
| What is stored | Only the SHA-256 of the token. |
| Asking again | A button on the site; the server allows one letter a minute. |
| What an unconfirmed person cannot do | Nothing is blocked. They see a strip asking to confirm, and get no reminders. We never write to an unconfirmed address except this letter - otherwise registering with a stranger's address would make us write to the stranger. |
| Accounts that existed before | Unconfirmed; the strip offers the letter. |

## Reminders

| Question | Decision |
|----------|----------|
| When | When the nearest proposed date still ahead is less than 24 hours away. |
| To whom | The creator and the guests who said yes, with a confirmed address. Not to those who have not answered or declined. |
| How often | Once per person per event. |
| Which clock | The time zone of the event - saved from the creator's browser at creation (new field). Events made before have none: the letter then shows UTC and says so. |
| Several proposed dates | Reminded about the nearest one, saying it is the first of N. There is no "final date" in the system yet. |
| How it runs | A timer inside the server, every ten minutes. Before sending, a row is written to `event_reminder` (unique per person per event): two servers cannot both send; if the letter fails the row is removed and the next run tries again. |

**The cost of the timer living in the server.** A sleeping server runs no timers. On a Heroku Eco
dyno, which sleeps after 30 minutes without requests, reminders go out only while the site is in
use, or late. A dyno that does not sleep (Basic), or an outside scheduler waking the server, fixes
it. This is a hosting decision, not a code one.

## Safety of letters

Everything a user typed (a name, a title) is escaped before it goes into the HTML of a letter, in
one place (`Letter`). Callers never write HTML. A subject is forced onto one line.

## Schema: migration `V005`

`user_account.email_verified_at`, table `email_verification`, `event.time_zone`,
table `event_reminder`.

## API changes

- New: `POST /auth/verify-email` (no token), `POST /account/email-verification/send`.
- `GET /profile/me` gains `emailVerified`.
- `POST /event/create` gains `timeZone`.

## Configuration

| Variable | Meaning | If absent |
|----------|---------|-----------|
| `MAGAIN_RESEND_API_KEY` | The key of the Resend account | Letters only go to the log |
| `MAGAIN_MAIL_FROM` | Sender, e.g. `Meet Again <hello@example.com>`; the domain must be verified in Resend | Resend's test sender, which delivers only to the owner of the Resend account |
| `MAGAIN_PUBLIC_URL` | The address of the site, for links in letters | `http://localhost:19000` |
