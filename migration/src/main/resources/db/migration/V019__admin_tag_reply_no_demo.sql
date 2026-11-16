-- Four things:
--   1. who is an administrator is written in the database, not in a setting;
--   2. a tag by a person's name, set by hand in the database only;
--   3. a chat message may answer another message;
--   4. the two demonstration accounts (Steve Jobs, Steve Wozniak) are removed for good.

-- 1. An administrator sees the analytics page. Set by hand:
--      update user_account set admin = true where email = '...';
--    WHY not a list of e-mails in a setting any more: the setting's default named a demonstration
--    account that anyone could sign in to by a link.
alter table user_account add column admin boolean not null default false;

-- 2. A short word shown by the person's name everywhere the name is shown ("Team", "Founder").
--    There is no way to set it from the application, on purpose: it is a mark we give.
--      update profile set tag = 'Team' where user_account_id = ...;
alter table profile add column tag text;
alter table profile add constraint profile_tag_check check (tag is null or char_length(tag) between 1 and 24);

-- 3. The message this one answers; in the same chat (checked by the application).
alter table chat_message add column reply_to__chat_message_id bigint references chat_message;

-- 4. The demonstration accounts. They were entered by a link with no password; the link is gone,
--    so the accounts are dead weight that still shows in other people's friends and plans.
--    Everything that belongs to them goes with them: the plans they created (with the chats of
--    those plans), their messages, friendships, notifications. This is the one place where rows
--    are deleted physically: these were never people.
create temp table demo_user on commit drop as
  select id from user_account where email in ('steve.jobs@hangelo.demo', 'steve.wozniak@hangelo.demo');

create temp table demo_event on commit drop as
  select id, chat_id from event where creator__user_account_id in (select id from demo_user);

create temp table demo_chat on commit drop as
  select chat_id as id from demo_event where chat_id is not null;

create temp table demo_message on commit drop as
  select id from chat_message
  where chat_id in (select id from demo_chat)
     or author__user_account_id in (select id from demo_user);

-- Members of other chats whose "read up to here" mark stands on a message that is going away:
-- the mark moves back to the newest message that stays, so nothing old turns "unread".
create temp table demo_moved_mark on commit drop as
  select id, chat_id from chat_member
  where last_read__chat_message_id in (select id from demo_message)
    and chat_id not in (select id from demo_chat)
    and user_account_id not in (select id from demo_user);

update chat_member set last_read__chat_message_id = null
  where last_read__chat_message_id in (select id from demo_message);

delete from chat_message where id in (select id from demo_message);

update chat_member member
  set last_read__chat_message_id = (select max(message.id) from chat_message message where message.chat_id = member.chat_id)
  where member.id in (select id from demo_moved_mark);

delete from chat_member
  where chat_id in (select id from demo_chat) or user_account_id in (select id from demo_user);

delete from notification
  where user_account_id in (select id from demo_user)
     or actor__user_account_id in (select id from demo_user)
     or event_id in (select id from demo_event);

delete from event_reminder
  where event_id in (select id from demo_event) or user_account_id in (select id from demo_user);

delete from event_suggestion where event_id in (select id from demo_event);

delete from event_time_option where event_id in (select id from demo_event);

delete from participant_preference
  where event_id in (select id from demo_event) or user_account_id in (select id from demo_user);

delete from invitation
  where event_id in (select id from demo_event) or invitee__user_account_id in (select id from demo_user);

delete from event where id in (select id from demo_event);

delete from chat where id in (select id from demo_chat);

delete from friendship
  where requester__user_account_id in (select id from demo_user)
     or addressee__user_account_id in (select id from demo_user);

delete from availability where user_account_id in (select id from demo_user);
delete from email_verification where user_account_id in (select id from demo_user);
delete from profile_interest where user_account_id in (select id from demo_user);
delete from profile_photo where user_account_id in (select id from demo_user);
delete from profile_prompt where user_account_id in (select id from demo_user);

-- A visit stays counted; it just belongs to nobody now.
update page_view set user_account_id = null where user_account_id in (select id from demo_user);

delete from profile where user_account_id in (select id from demo_user);
delete from user_account where id in (select id from demo_user);
