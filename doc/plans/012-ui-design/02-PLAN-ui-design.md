# PLAN 012: UI design documents

Documents only, in the front-end repository under `design/`. No code of the application changes.

1. Ask what a full designer prompt needs and cannot be guessed: deliverable, screen, look, brand,
   recipient, accent, audience, typography. The answers are in `01-prompt-ui-design.txt`.
2. `design/01-DESIGNER-PROMPT.md` - the master prompt for an AI design/code tool: the product,
   who uses it, voice, principles, references (BeReal, Apple and others - for principles, not to
   copy), hard rules, what to deliver per page.
3. `design/02-DESIGN-SYSTEM.md` - tokens for both themes (black, white, one blue, three state
   colours), type on the system font, space, layout by width, motion, every shared component,
   icons.
4. `design/pages/` - the shell and one file per page: purpose, data, desktop layout, elements by
   their existing `data-tst-id`, states, adaptation to tablet and phone, and a ready prompt.
5. `design/README.md` - the decisions, the map of files to routes and views, how to use, and the
   list of what the design asks for that the application does not do yet.
