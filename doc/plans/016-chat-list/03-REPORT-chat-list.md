# REPORT 016: the list of chats - last message, time, unread count, order

Done as planned, in both repositories. Nothing is committed, pushed or deployed.

## What changed

- Backend: `V013__chat_read.sql`; `ChatMember.lastReadChatMessageId`;
  `ChatMessageRepository.findNewestId`; `ChatService.markRead`, and the mark moving on writing and
  on joining; `ChatQuery.myChats` with the newest message, the count and the order; `ChatDto`;
  `POST /chat/{chatId}/read`. jOOQ classes regenerated.
- Front end: `src/stores/chats.ts` (new); `ChatsView` rows; `ChatView` tells the store when it is
  on the screen and when the person writes; the count beside "Chats" in `AppShell`.

## What was checked

- `gradlew testTz`: 0 failures in both time zones. New `ChatListApiTest` (7 tests): the newest
  message and the count of what others wrote; one's own messages are never unread; a silent chat
  has no last message; the order follows the newest message; marking read zeroes the count and
  later messages are new again; writing marks what stood above as read; a newcomer finds nothing
  unread but sees the history; marking read is for members only.
- `npm run build` passes.
- In Chrome against a local server with three chats: the list showed "Timur: Hi, How are you?",
  the time, a count of 2, the chats in order of their newest message, and the total beside
  "Chats". A message sent by the other person while the list was open moved its chat to the top
  with a count of 1 at once. Opening a chat cleared its count.

## Found and fixed on the way

- Opening a chat by its address on a fresh page did not clear its count: the list and the chat
  load together, and when the chat won, there was no row yet to clear. The store now clears the
  chat on the screen after every load of the list.

## Not checked

- The phone tab's count, and the light theme of the rows.
- "Yesterday" and older dates in the time column (all test messages were from today).

## Known limits

- The count on another device or tab of the same person is corrected on its next load of the
  list, not at once: reading is not signalled.
- No "unread from here" line inside an open chat.

## For the human

- Review; commit both repositories; deploy when decided (migration V013 runs on release).
