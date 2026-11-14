-- Public events: shown in a feed to everyone, open for anyone to join.

-- 1. Who an event is for.
--      PRIVATE - only the people its creator invites (by name or by its link). All events so far.
--      PUBLIC  - shown in the feed; any logged-in person can join it and enters its chat.
alter table event add column visibility text not null default 'PRIVATE'
  check (visibility in ('PRIVATE', 'PUBLIC'));

-- The feed reads the active public events, newest first.
create index event__feed on event (id desc) where visibility = 'PUBLIC' and status = 'ACTIVE';

-- 2. A fourth answer of a guest: REMOVED - taken out of the event by its creator. Unlike someone
--    who declined, a removed person cannot come back by joining again. An open event needs a way
--    to show somebody the door.
alter table invitation drop constraint invitation_status_check;

alter table invitation add constraint invitation_status_check
  check (status in ('PENDING', 'ACCEPTED', 'DECLINED', 'REMOVED'));
