-- A profile picture is a file kept by us, not a link to somewhere else.

-- 1. Links typed by users go away. A picture loaded from a foreign address tells that address who
--    looked at the profile (their IP), and its content can be swapped at any moment.
update profile set avatar_url = null where avatar_url is not null;

-- 2. The picture itself: one per account, already checked and re-encoded by the server.
--    `token` is the secret part of its address (/public/avatar/<token>); it changes with every
--    upload, so an old address stops working and browsers can cache a picture forever.
create table avatar (
  user_account_id bigint primary key references user_account,
  token           text        not null unique,
  content_type    text        not null check (content_type in ('image/jpeg', 'image/png')),
  content         bytea       not null,
  updated_at      timestamptz not null
);
