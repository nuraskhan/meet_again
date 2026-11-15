# REPORT 023: guests without geo, chat on the plan page, analytics, demo logins, America

Done as planned, in both repositories. Nothing is committed, pushed or deployed.

## What changed

- Backend: migration V017 (`page_view`; prices divided by 250); `EventType` default budgets in
  dollars; `DemoLoginService` and `POST /auth/demo/{who}`; new module `hangelo-mod/analytics`
  (`POST /public/visit`, `GET /admin/access`, `GET /admin/analytics`); settings
  `hangelo.demo-login.enabled`, `hangelo.admin.emails`.
- Front end: `DemoLoginView` (`/fl/:who`), `AnalyticsView` (`/admin/analytica`), the visit
  counter in `router.ts`, demo buttons on the log-in page, "Analytics" in the sidebar for admins,
  the chat inside `PlanView` (`ChatView` with `embedded`), the location block for the organiser
  only, dollars and US dates in `format.ts`, `dates.ts` and four views, Sunday first in the
  calendar.

## What was checked

- `gradlew testTz`: 0 failures in both time zones. New `DemoAndAnalyticsApiTest` (6): the two demo
  accounts and their friendship; the same account on a second login; only the two words work and
  no password opens them; visits, visitors and signed-in visits counted, a UTC day, 30 days with
  none missing; analytics closed to everyone but admins; an address with a query, a full URL and
  an odd visitor id refused.
- `npm run build` passes.
- In Chrome against a local server:
  - `/fl/steve-jobs` opened the application as Steve Jobs with Steve Wozniak as a friend and
    "Analytics" in the sidebar; a new coffee plan had the budget $6 - $16.
  - The plan page as the organiser: the location block, and the chat in the rail with a message
    and the field; the time reads "10:58 AM".
  - `/admin/analytica`: the tiles, the bars by day, the table of pages.
  - `/fl/steve-wozniak`, the same plan as a guest: no location block and no map; the chat is
    there; no "Analytics" link; `/admin/analytica` says "No such page" and the server answers 404.

## Not checked

- Writing a message in the rail's chat; the chat in the rail at phone width.
- The calendar with Sunday first, and the US date forms on every screen, by eye.
- The demo buttons on the log-in page (the links were used).
- Dark theme of the new pages.

## To know

- Guests no longer say where they are, so suggestions are measured from the organiser alone, and
  a guest's "N mi from you" is gone. The server still accepts a guest's location.
- Analytics are open to whoever uses the Steve Jobs demo link, until `HANGELO_ADMIN_EMAILS` names
  real administrators.
- `POST /public/visit` is open and unthrottled: anyone can inflate the figures.
- Venues are still those of Astana and Almaty, with names in Russian; an American audience would
  need venues of American cities.
- Numbers already typed into plans were divided by 250, whatever currency their authors meant.
- The demo accounts carry the names of real people; they are placeholders for a demonstration.

## For the human

- The fast-login links, once deployed: `<site>/fl/steve-jobs` and `<site>/fl/steve-wozniak`.
- Set `HANGELO_ADMIN_EMAILS` on Heroku to the real administrators' e-mails.
- Review; commit; deploy when decided (V017 runs on release).
