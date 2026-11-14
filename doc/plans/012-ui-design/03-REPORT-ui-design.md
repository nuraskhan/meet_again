# REPORT 012: UI design documents

Done as planned. In the repository `meet_again_ui`, directory `design/`:

- `README.md`, `01-DESIGNER-PROMPT.md`, `02-DESIGN-SYSTEM.md`;
- `pages/00-shell.md` and `pages/01` to `13`: welcome, auth, verify e-mail, invite, feed, plans,
  new plan, plan, friends, chats and chat, notifications, profile, person.

## What was checked

- Every route of `src/router.ts` has a page file.
- Element names in the page files are the `data-tst-id` values found in the views and components.
- Contrast calculated for the accent: white on `#0B5FFF` 5.1:1; `#4C8DFF` on black 6.6:1. The
  other colour pairs are not calculated; the prompt tells the tool to check each pair it uses.

## Not done

- No mockups and no change to the application: only documents were asked for.
- The prompts were not run through a design tool, so their output is not seen yet.
- The design asks for things the application does not do yet (sidebar, two-pane chats, live
  preview of a new plan and others): listed in `design/README.md`.

## For the human

- Review; commit in `meet_again_ui` (and this folder in the backend repository) when decided.
