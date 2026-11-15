# REPORT 022: take in the landing page work; ask for the location first

Done as planned. Not deployed.

## The merge

- `origin/main` of the front end had four commits not here: the new welcome page
  (`WelcomeView.vue`, `design/pages/01-welcome.md`), a one-line change in `AuthView.vue`, and a
  `CLAUDE.md` for that repository ("Before commit and push, please check that main is up to
  date"). Three local commits were not there. No file was changed on both sides; the merge had no
  conflicts.
- After the merge: `npm run build` passes; the welcome page was opened in Chrome - it shows with
  the brown accent, the theme switch and the logo; no console errors.

## The location

- `PlanNewView`: a section "Where you are" - "Use my location" (the browser asks the person), or
  a typed place. Someone who has already allowed the site to see their location is not asked
  again. After the plan is created the location is saved for it; the radius is the invitation's
  farthest distance. If saving fails, the plan stays and a message says why.
- `PlanView`: the location block moved above the venue and the suggestions.

## What was checked

- In Chrome: a plan created with a typed place opened with "Astana, left bank - up to 2 mi"
  under the summary, above "Where" and "Where to go".
- `npm run build` passes.

## Not checked

- "Use my location" in the form (the browser's permission prompt cannot be answered here).
- Phone width and the dark theme of the new section and of the new welcome page.

## To know

- A typed place alone gives no distances: they need the shared location. The form says so.
- The backend did not change in this piece.
