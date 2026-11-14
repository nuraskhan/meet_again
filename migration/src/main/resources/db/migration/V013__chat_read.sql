-- How far each member has read a chat: the list of chats shows how many messages are new.

-- The last message of the chat the member has seen. Empty = has seen none.
-- Everything written by others after it is "unread" for this member.
alter table chat_member add column last_read__chat_message_id bigint references chat_message;

-- Members there are today have "read" everything written so far: otherwise the day this is
-- released every chat would light up with its whole history as unread.
update chat_member member
set last_read__chat_message_id = (
  select max(message.id) from chat_message message where message.chat_id = member.chat_id
);
