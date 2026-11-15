# PLAN 024: Hangelo Plus

- The plan of a person is their privilege level, which exists already (`profile.privilege`). A new
  level `PLUS` stands for Hangelo Plus; `STANDARD` is the free plan. No second "plan" column: one
  fact, one place.
- `POST /profile/plus/start` moves the caller from the free plan to Plus; `/profile/plus/cancel`
  moves them back. A demonstration: no payment. A higher level (PRO, MAX) is never lowered.
- "Upgrade to Plus" stands above the person's name in the sidebar (their account menu), as an icon
  in the phone's top bar, and in the Account block of the profile. It opens the dialog of the
  brief: the name, the two prices, the four lines, "Start Plus", "Demo - no real payment".
- After "Start Plus": the badge "Plus" beside the name (sidebar and phone bar), the dialog closes.
- No limit on plans is introduced. Plus only adds; the free plan loses nothing.
- The welcome page gets a pricing section: Free and Hangelo Plus. The prices and the lines come
  from one file (`src/lib/plus.ts`) shared with the dialog.

Migration: `V018__privilege_plus.sql`.
