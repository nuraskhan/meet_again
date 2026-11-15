-- The creator can delete an event: it and its chat disappear for everyone.

-- Filled = deleted by its creator. Nothing is removed physically: the row stays, and every
-- reading of events leaves it out. (The members of its chat are marked as having left, so the
-- chat disappears from their lists by the rule that already exists.)
alter table event add column deleted_at timestamptz;
