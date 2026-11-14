-- A profile holds at most 10 photos, whatever the privilege level of the person.
--
-- The server checks the count before saving (Privilege.PHOTO_CEILING); this is the same limit where
-- it cannot be bypassed: positions run 1, 2, 3... without gaps, so a position above 10 is an
-- eleventh photo.

alter table profile_photo add constraint profile_photo__ceiling check (position <= 10);
