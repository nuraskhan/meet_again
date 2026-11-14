-- Joining a public event now needs the creator's approval.

-- 1. A fifth standing of a person in an event: REQUESTED - asked to join a public event from the
--    feed and is waiting for the creator to say yes or no. Not a guest yet: not in the chat, does
--    not see the event, takes no place in it.
alter table invitation drop constraint invitation_status_check;

alter table invitation add constraint invitation_status_check
  check (status in ('PENDING', 'ACCEPTED', 'DECLINED', 'REMOVED', 'REQUESTED'));

-- The creator's "who is asking" list reads the requests of one event.
create index invitation__requested on invitation (event_id) where status = 'REQUESTED';

-- 2. Three more things a person is told: someone asks to join my event; my request was approved;
--    my request was declined.
alter table notification drop constraint notification_kind_check;

alter table notification add constraint notification_kind_check
  check (kind in ('FRIEND_REQUEST', 'FRIEND_ACCEPTED', 'EVENT_INVITATION', 'INVITATION_ACCEPTED',
                  'INVITATION_DECLINED', 'GUEST_JOINED', 'GUEST_LEFT', 'REMOVED_FROM_EVENT',
                  'EVENT_CANCELLED', 'JOIN_REQUESTED', 'JOIN_APPROVED', 'JOIN_DECLINED'));
