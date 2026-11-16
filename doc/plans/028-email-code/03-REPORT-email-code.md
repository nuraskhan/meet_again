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
