# PLAN 014: the front end rebuilt on the designers' export

The export (`proto/hangelo-export` in the front-end repository): 13 static HTML pages that share
one stylesheet (tokens and components, classes `hg-*`), and the logo as SVG.

1. **Foundation**, done first and by one hand, because every page stands on it:
   - `src/assets/main.css` = the export's stylesheet as it is (PART 1) + what a running
     application needs and a static page does not (PART 2, classes `app-*`): the frame by screen
     width, page helpers, toasts, transitions, the colour of links that are components.
   - Line icons (`lucide-vue-next`) instead of emoji; `EventTypeIcon` decides the icon of an
     activity in one place.
   - `HangeloLogo`, the favicon; `UserAvatar`, `VerifyEmailBanner`, toasts.
   - `AppShell`: a sidebar (wide), the same collapsed to icons (medium), a top bar and a tab bar
     (phone).
2. **Pages**, converted in parallel, each to its export page: logic, API calls and every
   `data-tst-id` kept; the export's inline styles become scoped classes built on the tokens;
   every page also gets a phone layout the export does not draw.
3. **Rule for what the export shows and the application cannot do**: added if it is only
   presentation or simple client-side work; left out and listed if it needs data the server does
   not give.
4. Build, then look at every page in a browser against a local server with prepared data.
