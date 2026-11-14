-- A third status of an event: EXPIRED - its dates are behind. Set by a timer in the server, never
-- by a person.
--
--   ACTIVE    - open: people can be invited, join by the link, answer
--   CANCELLED - called off by its creator
--   EXPIRED   - the last proposed date has passed

alter table event drop constraint event_status_check;

alter table event add constraint event_status_check
  check (status in ('ACTIVE', 'CANCELLED', 'EXPIRED'));

-- The timer looks for active events every minute.
create index event__status on event (status);
