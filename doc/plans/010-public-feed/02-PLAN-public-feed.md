# PLAN 010: public invitations and the feed

The request is in `01-prompt-public-feed.txt`.

## How the request was read

| In the request | What is built |
|----------------|---------------|
| "public invitations for all who want to join" | An event is PRIVATE (as all were) or PUBLIC. A public one is shown in the feed and any logged-in person can join it. |
| "feed page" | `GET /feed/list`: active public events, newest first, by pages, with a filter by activity. |
| "check profile and see who is inviter" | Every feed card names the inviter with their picture and city and leads to their profile; the guests' faces lead to theirs. |
| "somehow it will be a public messenger" | Joining puts a person into the group chat of the event. A public event is thus an open chat room around a plan: anyone can enter it by joining. **The chat is not readable without joining** - see the open question below. |

## Decisions

| Question | Decision |
|----------|----------|
| Who chooses | The creator, when creating: "Only people I invite" (default) or "Everyone". It cannot be changed afterwards. |
| Joining | One tap in the feed: the person becomes a guest who is coming and enters the chat. The same as joining by the link, without needing the link. |
| A private event by its id | Answers "not found": an id is not an invitation. |
| Leaving | New: a guest who is coming can leave (`POST /event/{id}/leave`); they leave the chat and may join again. Before, saying yes could not be taken back. |
| An unwanted guest | New: the creator can remove a guest (`POST /event/{id}/guest/{userId}/remove`). The guest gets the status REMOVED, leaves the chat, loses the event from their list, and **cannot join again** - neither from the feed nor by the link. An event open to everybody needs this. |
| How many | At most 50 guests per event (invited and not answered, or coming). The feed marks a full event. |
| What the feed shows of an event | What the invitation says, the inviter, how many are coming and five of their faces, and where the reader stands (creator / coming / invited / left / removed / nothing). Not the chat, not the link, nothing guests said about their location. |
| What drops out of the feed | Cancelled and expired events; events of a closed account. |
| Invited friends | A public event can still have friends invited by name; they get the invitation as before. |

## Open question for the owner

Should the chat of a public event be readable (read-only) by people who have not joined, like a
public channel? It is not: today one joins first. Opening it is a small change, but it makes
everything written there public to every registered user, so it is the owner's call.

## Not in this change, and worth having before real use

A public surface invites abuse. Present: removal by the creator, the guest limit, the upload rules.
Absent: reporting a person or an event, blocking a person, moderation by us, requiring a confirmed
e-mail to post or join, a limit on how many public events one person can post.

## Schema: migration `V010`

`event.visibility` (PRIVATE by default, so every existing event stays private); a partial index for
the feed; the status REMOVED allowed for an invitation.

## API changes

- `POST /event/create` gains `visibility`; the event answer gains `visibility`.
- New: `GET /feed/list?eventType=&beforeId=&limit=`, `POST /event/{id}/join`,
  `POST /event/{id}/leave`, `POST /event/{id}/guest/{userId}/remove`.
- An invitation may now have the status `REMOVED`.
