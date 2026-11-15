-- A venue loaded from a directory comes with what the directory knows, and that is not always
-- the prices or a rating. "Unknown" is now empty, instead of a made-up number:
--   price_min / price_max empty - what one spends there is not known; the venue is never left
--                                 out for the budget;
--   rating empty                - nobody has rated it; it stands after the rated ones.
alter table place alter column price_min drop not null;
alter table place alter column price_max drop not null;
alter table place alter column rating drop not null;
