-- A fourth privilege level: PLUS - the plan a person takes themselves ("Upgrade to Plus").
-- For now it is a demonstration: no payment is taken; the level is simply switched.
alter table profile drop constraint profile_privilege_check;

alter table profile add constraint profile_privilege_check
  check (privilege in ('STANDARD', 'PLUS', 'PRO', 'MAX'));
