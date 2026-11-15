# REPORT 024: Hangelo Plus

Done as planned, in both repositories. Nothing is committed, pushed or deployed.

## What changed

- Backend: `Privilege.PLUS` (6 photos); migration V018; `ProfileService.startPlus`, `cancelPlus`;
  `POST /profile/plus/start`, `POST /profile/plus/cancel`.
- Front end: `src/lib/plus.ts`; `PlusDialog`; the upgrade button, the badge and the dialog in
  `AppShell`; the plan and "Back to the free plan" in `ProfileView`; the badge name in
  `PersonView`; the pricing section in `WelcomeView`.

## What was checked

- `gradlew testTz`: 0 failures in both time zones. New `PlusApiTest` (4): the free plan becomes
  Plus and stays so in the database; twice changes nothing and cancel goes back; a higher level is
  neither lowered nor dropped; a token is needed and only the caller changes.
- `npm run build` passes.
- In Chrome against a local server: the pricing section on the welcome page; as a demo account -
  "Upgrade to Plus" above the name, the dialog with the text of the brief, "Start Plus", after
  which the dialog closed, the badge "Plus" stood beside the name, the upgrade button was gone and
  a message said "You are on Hangelo Plus". No console errors.

## Not checked

- The phone's top bar (the icon and the badge), the dialog as a sheet on a phone, the profile
  page's button and "Back to the free plan", the dark theme.

## To know

- "plan = plus" is stored as `profile.privilege = 'PLUS'`.
- The four lines of Plus are promises of the product, not features that exist: plans are not
  limited for anyone, there are no saved crews, and reminders go to everyone already.
- The "Free" badge replaced "Standard" in the profile.
- Other people see the "Plus" badge on a person's page.

## For the human

- Review; commit; deploy when decided (V018 runs on release).
