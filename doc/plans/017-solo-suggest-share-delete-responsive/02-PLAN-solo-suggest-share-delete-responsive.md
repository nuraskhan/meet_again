# PLAN 017: places when planning alone, a share button, the site icon, folding the sidebar, deleting, phone layouts

How each line of the request was read, and what is done for it.

| Asked | Read as | Done |
|-------|---------|------|
| Suggest place - generate solo | Places for someone planning alone, without first filling the location form | `POST /event/{id}/suggestion/refresh` takes an optional position. If the caller has no saved location, it is saved as theirs and places are looked for at once. The page asks the device for the position when nobody is on the map. A location chosen by hand is never overwritten. |
| Share link - hide, a button instead of the long text | The address is noise | "Share" (the device's share sheet where there is one, copying otherwise) and "Copy link". The address stays as one quiet selectable line for a browser that can do neither. |
| Vite - remove the logo, put Hangelo's | The site icon was the template's default | `favicon.ico` removed; the page uses the Hangelo mark (`favicon.svg`). |
| A button hiding the left bar | Give the page the room | A button beside the logo folds the sidebar to its icons; remembered in the browser. |
| Delete chats, or delete events to delete chats | A chat lives with its plan | The creator deletes a plan (`POST /event/{id}/delete`): it and its chat disappear for everyone; an active one is cancelled first so guests are told. A guest takes a plan that is over or cancelled, and its chat, off their own lists (`/leave`). Nothing is removed physically. |
| Make it adaptive | The phone and tablet layouts had never been seen | Every page looked at in a 390 px frame and the problems found fixed. |

Migration `V014__event_deleted.sql`: `event.deleted_at`.
