# PLAN 002: invite by link, several proposed dates

The request is in `01-prompt-invite-by-link.txt`. What already existed from plan 001: title,
activity (event type), one optional date, inviting friends from the list, everything stored.

## What is new

| Requirement | Decision |
|-------------|----------|
| Proposed dates and time | An event has zero to ten proposed dates instead of one. Zero = "to be agreed in the chat". There is no voting: people agree in the chat. |
| Invite by link | Every event gets a secret token; the link is `<site>/i/<token>`. Only the creator is given the token. |
| Friends from the list OR by link | The list of invitees may be empty. The chat is created anyway, with the creator alone. |
| Open without registration | `GET /public/event/{token}` needs no login. It returns what the invitation says, the creator's name and two counts. No ids, no guest names. |
| Accepting needs registration | `POST /event/join/{token}` needs a token like every other endpoint. The UI sends a visitor to register or log in and brings them back to the invitation. |

## Decisions

1. **The token is the only protection of the public page**, by design: 18 random bytes (144 bits),
   base64url. Anyone who has the link can read the invitation.
2. **Joining by link needs no friendship.** The link is the invitation. It does not make people
   friends either.
3. **Joining = accepting.** The person becomes a guest with the answer "yes" and enters the chat.
   It also works for an invitee who has not answered and for one who declined (they come back into
   the chat). Calling it twice changes nothing.
4. **A cancelled event still opens by its link** and says it is cancelled; joining it is refused.
5. **Schema: migration `V002`.** `event.share_token` (existing events get one),
   table `event_time_option`, the old `event.starts_at` is copied into it and dropped.

## API changes

- `POST /event/create`: `startsAt` is replaced by `proposedTimes` (a list); `inviteeIds` is optional.
- Event in every answer: `startsAt` is replaced by `proposedTimes`; new `shareToken` (null unless
  the caller is the creator).
- New: `GET /public/event/{shareToken}`, `POST /event/join/{shareToken}`.

This is a breaking change for the UI: the two must be deployed together.
