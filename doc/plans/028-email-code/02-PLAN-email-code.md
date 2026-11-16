# Plan: the e-mail is confirmed by a code typed after logging in

## What is asked

1. An account whose e-mail is not confirmed is thrown back to the login page.
2. After logging in, if the e-mail is not confirmed, the person is taken to its confirmation.
3. There they type a confirmation code.

## Decisions

- **The letter carries a six-digit code** as well as the link. The link stays: it costs nothing and
  works when the letter is opened on another device.
- **`POST /account/email-verification/confirm {code}`**, with the token of the unconfirmed account;
  open to it in `EmailVerifiedFilter`, like "send again".
- **Five wrong codes per letter**, then that letter accepts no more codes and a new one has to be
  asked for (once a minute, as before). A six-digit code is one of a million; without a limit it
  could be tried through. The wrong try is counted although the request fails
  (`noRollbackFor`).
- **Stored as a hash**, like the token of the link, with the account id mixed in.
- **Logging in with an unconfirmed e-mail sends a fresh letter**, so the page that asks for the code
  has one to ask for. If a letter went out less than a minute ago, none is sent and the login still
  succeeds.
- **The web application does not remember an unconfirmed account**: its token is kept in memory
  only. A reload, or a remembered token of an account that turns out unconfirmed, leads to the
  login page; logging in leads to the code.

## Checks

- `gradlew testTz`; `npm run build`.
- In the browser: register, a wrong code, reload (login page), log in (code page), the right code
  (inside).
