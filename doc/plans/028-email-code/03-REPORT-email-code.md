# Report: the e-mail is confirmed by a code typed after logging in

## Done

Server:

- Migration `V020__email_verification_code.sql`: `email_verification.code_hash`,
  `email_verification.failed_attempts`. Typed database classes regenerated.
- The confirmation letter carries a six-digit code ("Your confirmation code: 123456") and, as
  before, the link.
- `POST /account/email-verification/confirm {code}`: confirms with the code of the newest letter.
  `400 wrong_code`, `400 code_expired` (used, older than 24 hours, or a letter from before codes),
  `429 too_many_attempts` after five wrong codes for one letter. Open to an unconfirmed account in
  `EmailVerifiedFilter`.
- `POST /auth/login` of an unconfirmed account sends a fresh letter; when one went out less than a
  minute ago, none is sent and the login succeeds all the same.

Web application:

- `ConfirmEmailView` is now "Enter the code": one field for six digits (pasted text is reduced to
  its digits; a phone may offer the code from the letter), "Confirm", "send it again", "Log out".
  It still notices by itself when the link was opened elsewhere.
- The token of an unconfirmed account is kept in memory only. A reload leads to the login page; a
  remembered token whose account turns out unconfirmed is dropped the same way.
- The destination asked for before the login (`next`) is carried through the code step.
- `RELEASES.md`: card `v0.4.0`, pending until deployed.

## Checked

- `gradlew testTz`: BUILD SUCCESSFUL. New tests in `EmailVerificationApiTest`: the right code opens
  the application and uses up the link; a wrong code is refused and after five even the right one
  is; not six digits and no token are refused; one person's code does not confirm another; a login
  sends a fresh letter after the pause and none within it.
- `npm run build` passes.
- In Chrome against the local stack: registration leads to the code page; "12a 34" becomes "1234"
  and the button stays off; a wrong code shows the refusal; a reload leads to the login page;
  logging in leads back to the code page; the code from the letter (read from the server's log,
  where a developer's machine writes letters) lets the person in.

## Not done, and why

- **The letter was not received in a real mailbox**: locally letters are only written to the log.
  How the code looks in a mail program is seen after the deploy.
- **The link in the letter was kept**, though only the code was asked for: it is the way in for
  someone who opens the letter on another device, and removing it would break letters already sent.

## Left to the human

- Commit, push, deploy (the server first: the page needs the new endpoint).

## Fixed afterwards: old sessions hung; the developers section moved to /dev

Reported after the deploy of v0.4.0: sessions remembered from before confirmation became required
hung - a message "confirm your e-mail" at the bottom, and no way anywhere, not even to the login
page. And asked: the developers section belongs in the top bar, opening a page of its own, `/dev`.

Why they hung. A remembered token let the router into the application at once; the application's
requests were refused (`403 email_not_verified`, shown as messages). The session was then ended,
but the "go to the login page" step looked at the current page while the first page was still
loading - "nowhere" is not a private page, so nobody was sent anywhere. And while the token was
still remembered, the login page itself sent the person back into the application.

Done, in the web repository:

- The router asks the server whom a remembered token belongs to BEFORE the first private (or
  guest-only) page: an unconfirmed or expired session is ended there and the page opened is the
  login page.
- The request layer tells the auth store about `403 email_not_verified`; a remembered session is
  ended on it (a session waiting for its code is left alone).
- The "leave the private pages" step waits until the router has a page.
- `DevView` at `/dev` carries what the section carried (three cards, the latest release, the
  repositories); the welcome page has a "Developers" link in its top bar instead of the section.
  The rule about release cards names the page for developers instead of the welcome page.
- `RELEASES.md`: card `v0.4.1`, pending until deployed.

Checked in Chrome against the local stack, with a remembered token of an unconfirmed account put
into the browser by hand: opening `/plans` lands on `/login?next=/plans` with the token gone and the
form shown; opening `/` stays on the welcome page with the token gone, and "Log in" opens the login
page; "Developers" opens `/dev`; the welcome page has no developers section any more. At 390 px:
no sideways scrolling on `/` and `/dev`, the "Developers" link is on the screen. `npm run build`
passes. Nothing changed on the server.
