-- The confirmation letter now carries a six-digit code as well as the link: the person types the
-- code into the page they are on, instead of leaving it for the mail and coming back.

-- SHA-256 of the code, in hex; the code itself is never stored, like the token of the link.
-- Empty for letters sent before this: those can still be confirmed by their link.
alter table email_verification add column code_hash text;

-- How many wrong codes were typed against this letter. A code has only a million values, so the
-- tries are counted: after a few wrong ones the letter stops accepting codes and a new letter has
-- to be asked for.
alter table email_verification add column failed_attempts int not null default 0;
