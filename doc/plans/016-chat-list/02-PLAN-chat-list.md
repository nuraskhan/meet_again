# PLAN 016: the list of chats - last message, time, unread count, order

## Backend

- Migration `V013__chat_read.sql`: `chat_member.last_read__chat_message_id` - the last message
  of the chat the member has seen. Members there are today are set to the newest message, so the
  release does not mark whole histories as unread.
- "Unread" for a member: written by someone else, after the last message they have seen.
- The mark moves: when the member opens the chat (`POST /chat/{chatId}/read`), when they write,
  and to the newest message when they join or come back (history is not news).
- `GET /chat/list` carries for each chat `lastMessage` (the same shape as a message) and
  `unreadCount`, and is ordered by the newest message; a chat nobody has written in stands by the
  moment it was created.

## Front end

- `stores/chats.ts`: the list, kept current. A `chat.message` signal puts the message into its
  chat, moves the chat to the top and adds to its count - unless that chat is on the screen, which
  is then marked read.
- A row of the list: the name and the time of the newest message; under them "Timur: Hi, how are
  you?" and the count of new messages. The time is the hour today, "Yesterday", then the date.
- The total of new messages beside "Chats" in the navigation (sidebar and phone tab).
