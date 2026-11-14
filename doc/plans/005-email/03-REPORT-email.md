# REPORT 005: e-mail - confirming an address, reminders about upcoming events

Built as described in `02-PLAN-email.md`.

## Backend

- New module `lib/mail`: `EmailSender`, `ResendEmailSender`, `LogEmailSender`, `Letter` (the one
  look and the escaping), `MailProperties`.
- Migration `V005__email_verification_and_reminders.sql`; jOOQ classes regenerated.
- `lib/model`: `EmailVerification`, `EventReminder`, `UserAccount.emailVerifiedAt`,
  `Event.timeZone`, with repositories and RND factories.
- `auth` module: `EmailVerificationService`, `AccountController`; `AuthController` sends the letter
  at registration and takes the link back.
- `event` module, package `reminder`: `EventReminderJob`, `ReminderQuery`, `ReminderLog`,
  `ReminderLetter`, `ReminderSchedulingConfig`.
- `profile` module: `emailVerified` in the caller's profile.

## Front end (repository `meet_again_ui`)

- A strip under the top bar while the address is not confirmed, with "Send it again".
- The page `/verify-email`, which the link in the letter opens.
- A new plan is sent with the time zone of the browser.

## What was checked

- `gradlew testTz`: 386 test executions, 0 failures (193 tests in two time zones). New:
  `EmailVerificationApiTest` (10), `ReminderApiTest` (9), `ReminderLetterTest` (5), `LetterTest` (3).
  Among them: registration survives a failing mail provider; a link works once; an expired and an
  unknown link get the same answer; one letter a minute; reminders go to the creator and to guests
  who said yes and to nobody unconfirmed; once however often the job runs; nothing for a cancelled
  event or one without dates; a failed letter is retried; the time in a reminder is written out for
  New York and Los Angeles; a name with markup is escaped in the letter.
- Locally, without a Resend key: registered a user, the letter appeared in the server log, the
  profile said "not confirmed", an immediate second request was refused (429); in a browser the
  strip showed, the link opened "Your e-mail is confirmed", and opening it again said it no longer
  works.

## Not checked

- **Nothing was sent through Resend.** There is no key yet, so `ResendEmailSender` has never made a
  real call. Its request follows Resend's documented format; the first real letter is the test.
- The timer itself (`@Scheduled`) firing in a running server: the tests call the job directly.

## Known limits

- Reminders depend on the server being awake (see the plan).
- No way to turn reminders off, and no unsubscribe link in the letter.
- Only one reminder, a day before. Nothing when someone is invited, answers, or writes in the chat.
- The address cannot be changed, so there is no re-confirmation flow.
- Opening a confirmation link twice shows "does not work any more" the second time, although the
  address is confirmed.

## To switch real sending on

1. A Resend account, a verified sending domain, an API key.
2. On the Heroku app `meet-again-api`: `MAGAIN_RESEND_API_KEY`, `MAGAIN_MAIL_FROM`,
   `MAGAIN_PUBLIC_URL=https://meet-again-ui-112b5408b3d1.herokuapp.com`.

## Not done by the agent

Not committed, not deployed.
