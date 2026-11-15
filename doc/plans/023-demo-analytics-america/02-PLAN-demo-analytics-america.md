# PLAN 023: guests without geo, chat on the plan page, analytics, demo logins, America

| Asked | Done |
|-------|------|
| No map and no geo on a plan for guests | The block "Your location and distance" is shown to the organiser only. |
| The chat in the right panel of a plan | The plan's chat stands in the rail under People: messages and the field, live. The button that opens it full size stays. |
| Analytics of visits at /admin/analytica | Every page opened is recorded (table `page_view`): the pattern of the page, a random id of the browser, who was signed in. The page shows page views, visitors, signed-in views, new accounts, new plans, views by day, the pages opened most; 7, 30 or 90 days. Admins only. |
| Fast login as Steve Jobs and Steve Wozniak | `/fl/steve-jobs` and `/fl/steve-wozniak` sign in with no password. The accounts are made on first use and are friends. Two buttons on the log-in page do the same. |
| Currency and the rest aimed at America | Prices in US dollars ("$20 – $60"); dates and times as written in the US ("Nov 20, 7:00 PM"); the calendar week starts on Sunday. Miles were there already. |

## What "admin" is

E-mails listed in `hangelo.admin.emails` (`HANGELO_ADMIN_EMAILS`). Not set = the demonstration
account Steve Jobs alone. For everyone else the page and its figures "do not exist" (404).

## What is recorded about a visit, and what is not

The pattern of the page (`/plans/:eventId`), never its address - an address can carry the token of
an invitation link. A random id kept in the browser. No IP address, no browser details.

## Dollars

Default budgets are rewritten (a bathhouse: 20-60). Existing plans and places are divided by the
same factor, 250, in migration V017 - a change of scale for demonstration data, not an exchange
rate.

## Demo logins: the safety of it

Anyone with the link is in as that account. That is the purpose; nothing private belongs there.
Switched off with `HANGELO_DEMO_LOGIN=false`. No password opens these accounts.
