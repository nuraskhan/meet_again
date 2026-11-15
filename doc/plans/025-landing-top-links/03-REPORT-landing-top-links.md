# REPORT 025: "Pricing" and "Try demo" at the top of the welcome page

Done as planned. Not committed, not pushed, not deployed.

## The pull

- Backend: one new commit on GitHub, "Add frontend link to Readme.md" (README.md, 2 lines);
  taken by fast-forward. Front end: nothing new.

## What changed

- `WelcomeView`: a `nav` in the top bar with "Pricing" (a button that scrolls to the plans and
  shows them at once) and "Try demo" (a link to the demo login); the pricing section got an id.

## What was checked

- `npm run build` passes.
- In Chrome: both entries beside the logo on a wide screen; "Pricing" brought the plans into
  view; "Try demo" points at `/fl/steve-jobs`. At 760 px they stay in the bar; at 390 px they
  stand on a line of their own under it. No sideways scrolling at either width.

## Not checked

- Pressing "Try demo" on this page (the demo link itself was used before, in 023).
- The dark theme.

## To know

- "Try demo" signs the visitor in as the Steve Jobs demonstration account - the same account for
  everyone who presses it, and, until `HANGELO_ADMIN_EMAILS` is set, the one that may see the
  analytics.
