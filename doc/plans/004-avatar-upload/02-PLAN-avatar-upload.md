# PLAN 004: profile picture as an uploaded file

The request is in `01-prompt-avatar-upload.txt`.

**How the last part was read.** "Add some vulnerabilities or some prohibited files append" is taken
to mean: protect the upload against the known ways to abuse one, and refuse prohibited kinds of
file. Nothing was made vulnerable on purpose.

## Decisions

| Question | Decision |
|----------|----------|
| The link | Gone. The profile update no longer accepts an address, existing addresses are cleared, and the UI never loads a picture from another site. |
| Where the file lives | In the database (`avatar.content`). The disk of a Heroku server is wiped on every restart. |
| Which files | JPEG and PNG only, up to 2 MB. |
| How it is shown | `GET /public/avatar/<token>`, open without login because an `<img>` tag cannot send a token. The token is random (144 bits) and new on every upload. |

## The protections, each against one attack

| Attack | Protection |
|--------|------------|
| A script, page, SVG, archive or program renamed to `photo.png` | The type is decided by the first bytes of the content. The file name and the type the browser declares are never read. |
| A path in the file name (`../../etc/passwd`) | The name is not used for anything; nothing is written to disk. |
| A tiny file declaring an enormous picture, to exhaust memory | The size in pixels is read from the header and checked before decoding (at most 8000 on a side, 24 megapixels). |
| A huge upload | 2 MB per file, 3 MB per request, refused before the controller runs. |
| A file that is a valid picture AND carries something else after the image data | The upload is never stored. It is decoded and encoded again; only pixels survive. |
| Private data inside a photo (EXIF, including the GPS position) | Dropped by the same re-encoding. |
| A broken file built to crash the decoder | Any decoding failure is one answer, "cannot be read as a picture"; only the decoder of the recognised format ever sees the bytes. |
| The browser treating the served file as a page | Served with our own content type (`image/jpeg` or `image/png`), `nosniff`, and a content security policy that allows nothing. |
| Guessing the address of somebody's picture | The address is a random token, not the user id. |
| A link to another site as a picture (it reveals who looked) | Not possible to set; the UI refuses any address that is not `/public/...` of our server. |

## Schema: migration `V004`

`profile.avatar_url` is cleared where set; new table `avatar` (one row per account).

## API changes

- `POST /profile/me` no longer takes `avatarUrl`.
- New: `POST /profile/me/avatar` (form upload, part `file`), `POST /profile/me/avatar/remove`,
  `GET /public/avatar/{token}`.
- `avatarUrl` in every answer is now the server path of the uploaded picture.
