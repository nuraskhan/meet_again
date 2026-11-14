# REPORT 013: rebrand to Hangelo

Done as planned, in both repositories. Nothing is committed, pushed or deployed.

## What was checked

- No `magain` or `meet_again` left in tracked backend files outside `doc/plans`, except the two
  lines of `CLAUDE.md` that name the GitHub repository and the token variable as they still are.
- jOOQ regenerated against the renamed database: no differences from the renamed files.
- `gradlew clean`, then `gradlew testTz`: 514 test executions (257 tests in two time zones),
  0 failures.
- Front end: no old name left outside `node_modules`, `dist` and the local `.idea`;
  `npm run build` passes.
- The local database kept its data: `hangelo` and `hangelo_test` exist, migrations report nothing
  to apply.

## Not checked

- The application was not started and looked at in a browser after the rename.
- The logo prompt was not run through an image tool.

## Consequences to know

- **Heroku will not start the new code as it is.** It has `MAGAIN_JWT_SECRET`,
  `MAGAIN_RESEND_API_KEY`, `MAGAIN_MAIL_FROM`, `MAGAIN_PUBLIC_URL`; the code now reads
  `HANGELO_*`. The same values must be set under the new names before the next deploy, and the
  sender name in `HANGELO_MAIL_FROM` changed to "Hangelo".
- People signed in on a browser are signed out once: the storage keys were renamed.
- IntelliJ must reload the Gradle project: the modules moved.
- The logo's two colours (earth brown, red) stand beside an interface that is black, white and
  blue. The design system keeps them inside the logo only.

## For the human

- Decide and do, if wanted: rename the folders on disk, the GitHub repositories, the Heroku apps
  (their addresses change), the variables `MEET_AGAIN_KEY`, `MEET_AGAIN_HEROKU_KEY`,
  `MEET_AGAIN_RESEND_KEY`; then `CLAUDE.md` section 8 follows.
- Review; commit both repositories; deploy when decided (with the config vars above).
- Run `design/logo/LOGO-PROMPT.md` with the sketch through an image tool; put the chosen files
  into `design/logo/` and `public/`.
