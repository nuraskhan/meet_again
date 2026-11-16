# Report: eleven fixes (confirmation required, chat replies, no demo login, admin and tag in the database, calendar, brainb.kz/hangelo)

## Done

### Server (this repository)

- **Migration `V019__admin_tag_reply_no_demo.sql`**: `user_account.admin`, `profile.tag` (1 to 24
  characters), `chat_message.reply_to__chat_message_id`; deletes the two demonstration accounts
  with everything that belongs to them. Typed database classes regenerated.
- **Confirmation required**: `auth/EmailVerifiedFilter` refuses every request of an unconfirmed
  account with `403 email_not_verified`, except `GET /profile/me` and
  `POST /account/email-verification/send`. `AuthResponse` carries `emailVerified`.
- **No demonstration login**: `DemoLoginService`, `POST /auth/demo/{who}` and the setting
  `hangelo.demo-login` are gone.
- **Admins**: `AnalyticsService.isAdmin` reads `user_account.admin`; the setting
  `hangelo.admin.emails` is gone.
- **Tag**: in the profile (own and others'), the list of friends, the guests of a plan and the
  members of a chat. Not accepted by the profile update.
- **Answering a message**: `PostMessageRequest.replyToId`, `ChatMessageDto.replyTo`
  (id, author, the first 140 characters). An answer to a message of another chat is refused
  (`400 reply_not_found`).
- **Sample people of `make data`** count as confirmed, also those loaded earlier.
- **IntelliJ**: `.idea/dataSources.xml` with `hangelo (local)` and `hangelo_test (local, tests)`.

### Web application (`meet_again_ui`)

- Served under `/hangelo/`: `base` in `vite.config.ts`, `BASE` in `server.js`, the router, the
  address of the server (`/hangelo/api`), invitation links. Addresses outside `/hangelo` are
  redirected into it. `deploy/nginx-brainb.kz.conf`: the block for the nginx of brainb.kz.
- `ConfirmEmailView` ("Check your inbox"): the only page for an unconfirmed account; it lets the
  person in by itself once the link was opened. The auth store keeps such a token apart.
- No demonstration login: `DemoMenu`, `DemoLoginView`, the buttons on the login page, `/fl/:who`.
- Notifications are read when the page is opened.
- Chat: a reply button by every message, the answered message quoted in the answer (a click goes
  to it), "Replying to" above the field.
- Calendar: ruled month, a chip per plan (icon, short time, name), named bands; dots when narrow.
- Reliability: the air emoji and the percentage.
- Phone: no zoom on a double tap, none on focusing a field.
- `UserTag` by the name: sidebar, person page, friends, guests of a plan, chat members.

## Checked

- `gradlew testTz`: BUILD SUCCESSFUL. New tests: `AnalyticsApiTest`, `ChatReplyAndTagApiTest`,
  three tests of the closed door in `EmailVerificationApiTest`. `ApiTestBase.register()` now
  confirms the e-mail by the link from the captured letter; `registerUnconfirmed()` does not.
- The migration ran on the local database that held both demonstration accounts with plans, chats
  and friendships: both gone, no error.
- `npm run build` passes. In Chrome against the local stack: login, the calendar, a reply sent
  from the page, the bell going from 2 to 0 on opening and staying 0 after a reload, registration
  landing on "Check your inbox" and staying there when a private address is opened, the tag by the
  name. At 390 px: no sideways scrolling on the calendar, a chat and the feed; `touch-action` is
  `manipulation`; the chat field has 16px text.
- `npm run dev` serves `/hangelo/` and forwards `/hangelo/api`.

## Not done, and why

- **brainb.kz itself.** Its nginx is not ours to change from here; the block is ready.
- **`HANGELO_PUBLIC_URL` on Heroku** still names the Heroku address. Until brainb.kz forwards
  `/hangelo/`, changing it would send people to a page that does not exist. Links built from the
  old value keep working: they are redirected into `/hangelo/`.
- **A production data source in IntelliJ.** The file is committed, and the address and user of the
  production database do not belong in the repository.
- **Tag in every place a name appears.** Not in chat bubbles, notifications, the feed and search
  results: the name there comes from other queries. The five places above are where a person is
  looked at.
- **The double-tap zoom was not tried on a real phone**: only the style rule was verified.

## Left to the human

- Commit, push, deploy.
- After the deploy: mark the administrators and tags in the production database
  (`update user_account set admin = true where email = '...'`;
  `update profile set tag = 'Team' where user_account_id = ...`).
- Add the block of `deploy/nginx-brainb.kz.conf` to the nginx of brainb.kz, then set
  `HANGELO_PUBLIC_URL=https://brainb.kz/hangelo`.

## Added afterwards: a section for developers

Asked on the same day: "add to the landing a section for developers, where one can leave a bug
report and create issues, and anyone who wants can contribute on GitHub (I will check every merge
request)".

- Welcome page: section "Built in the open" before the closing one, with three cards - "Report a
  bug" (a new issue), "Open issues", "Contribute" (the contributing guide) - and links to both
  repositories. All open GitHub in a new tab.
- `CONTRIBUTING.md` in both repositories: how to report a bug, propose an idea, send a pull
  request, what to check before sending, and that every pull request is reviewed by the maintainer
  before it is merged.
- The words say "the code is public", not "open source": neither repository has a licence file,
  and without one nobody is formally allowed to reuse the code. Choosing a licence is the owner's.
- Checked: both repositories are public (GitHub answers without a token); the section at a wide
  screen and at 390 px; `npm run build`.

## Added afterwards: release cards

Asked on the same day: keep batches and versions and when each was applied, in git too; a rule in
CLAUDE.md that every re-deploy makes a card; the developers section shows only the last card and a
link to git.

- `RELEASES.md` in the web repository: one card per deploy (version, `Applied:` in UTC, the
  commits, the changes under Added / Changed / Fixed / Removed). Two cards to begin with: `v0.1.0`
  (everything before the log, the deploy of 2026-11-15) and `v0.2.0` (this batch, `Applied: pending`
  until it is deployed).
- `src/lib/releases.ts` reads that file at build time; the welcome page shows the newest card that
  is not pending, under the three developer cards, with "All releases on GitHub".
- The rule: "Releases: a card for every deploy" in the web repository's `CLAUDE.md` (steps, right
  and wrong, why) and section 9 of this repository's `CLAUDE.md` pointing to it.
- Checked: the card on the page at a wide screen and at 390 px (it shows v0.1.0 while v0.2.0 is
  pending); `npm run build`.

## Changed afterwards: hangelo.brainb.kz instead of brainb.kz/hangelo/

brainb.kz turned out to have no server of its own: at ps.kz there is only DNS, the bare domain is a
redirect page of the registrar, and a bare domain cannot be pointed at Heroku with an ordinary
record. `www.brainb.kz/hangelo/` was possible but, in the owner's words, looked bad. Decided the
same day: the application gets its own address, `hangelo.brainb.kz`, and lives at its root again.

- Web repository, release v0.3.0: `base` is `/` again; `server.js` leads addresses that still carry
  `/hangelo` to the same place without it; `deploy/nginx-brainb.kz.conf` is removed.
- Nothing changed in this repository. `HANGELO_PUBLIC_URL` becomes `https://hangelo.brainb.kz` once
  the DNS record exists.
