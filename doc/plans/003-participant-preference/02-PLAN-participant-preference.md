# PLAN 003: participant preferences, US units

The request is in `01-prompt-participant-preference.txt`. Neither value existed in the system, so
both are added.

## Decisions

| Question | Decision |
|----------|----------|
| Whose preference | A guest's: someone invited by name or joined by the link, who has not declined. Not the creator: their wish is the distance of the event itself. |
| Per what | Per event. The same person can be at home for one plan and at work for another. |
| Current location | Coordinates from the device (the browser asks the person), or a typed text, or both. At least one. A typed text is stored as typed: it is not turned into coordinates. |
| Travel radius | Whole miles, 1 to 500. Required. |
| Who sees what | The owner gets everything back. The other participants get the radius, the typed text, and the fact that a device location was shared. Coordinates never leave the server for anyone but their owner. The public page of an event shows no preferences at all. |
| US units | Distances are miles everywhere. The preferred distance of an event was in kilometres: its columns, fields and defaults are now miles, and existing values are converted. |
| When it is asked | It is not a condition of accepting. After accepting, the guest lands on the plan page, where the form is highlighted until it is filled. |

## Schema: migration `V003`

- `event.distance_min_km` / `distance_max_km` renamed to `..._mi`, values converted (x 0.621371,
  rounded).
- New table `participant_preference`: one row per guest per event; check constraints keep latitude
  and longitude together, inside the globe, and require a place of some kind.

## API changes

- Everywhere: `distanceMinKm` / `distanceMaxKm` are now `distanceMinMi` / `distanceMaxMi`
  (and `defaultDistance...Mi` in the event types).
- New: `POST /event/{eventId}/preference` with `latitude`, `longitude`, `locationText`,
  `travelRadiusMi`.
- Event answer: new `myPreference`; each invitee gets `travelRadiusMi`, `locationText`,
  `locationShared`.

Breaking for the UI again: backend and UI go out together.

## Not in this change

- Money stays as it was: whole numbers, shown with one currency sign set in the UI.
- Nothing is computed from the locations yet (no distance between people, no suggested places).
