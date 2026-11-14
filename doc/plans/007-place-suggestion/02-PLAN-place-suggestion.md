# PLAN 007: suggesting places from where everyone is coming from

The request is in `01-prompt-place-suggestion.txt`.

## What is added

| Piece | Decision |
|-------|----------|
| Places | A table `place`: name, category, address, coordinates, rating 0-5, price range, description, website, phone. The category is one of the event types. |
| Coordinates of everyone | The creator can now give a location and travel radius too (before, only guests could): a place is chosen from where EVERYONE is coming from. |
| Which locations count | The creator and every guest who has not declined - those of them with coordinates (a pin on the map, or the device). A place typed as text cannot be measured and is skipped. |
| Which places | Same category as the event (any category for the type OTHER), not deactivated, inside a box around those people widened by the largest travel radius. |
| The order | 1) within everyone's own travel radius; 2) prices overlapping the event's budget; 3) the higher rating; 4) the smaller average distance. |
| How many | Three. If nothing is within everyone's reach, the nearest are still offered, marked "too far for someone". |
| Details shown | Name, address, rating, description, price range, average and farthest distance, the caller's own distance, a link to the map, website, phone. |
| When it is computed | By the button "Suggest places", for any participant; and by itself the first time the location of everyone coming (the creator and the guests who said yes) is known. |
| Saved once | The result is kept in `event_suggestion` and shown as kept on every opening of the plan and in the list. It changes only when someone presses the button again. `event.suggested_at` says when; filled with no rows = looked, found nothing. |

## Distances

As the crow flies (the haversine formula), in miles, rounded to a tenth. Not by road: a real trip
is longer.

## Privacy

A suggestion carries the average, the farthest, and the caller's own distance - never who is how
far, and never anybody's coordinates. The page opened by a link shows no suggestions.

## Where places come from

**Not decided - this needs the owner's answer.** The table is empty in production. For local work
`make data` loads twenty INVENTED places around New York City (`SamplePlaces`), each marked
`sample = true` and shown with a "Sample" mark. They are not real businesses and are not loaded
anywhere but a developer's database.

Ways to fill it for real: an admin screen (the planned `magain-admin`), a file import, or
OpenStreetMap (real names and coordinates, no ratings).

## Schema: migration `V007`

Tables `place` and `event_suggestion`; `event.suggested_at`.

## API changes

- New: `POST /event/{eventId}/suggestion/refresh`.
- `POST /event/{eventId}/preference` now also accepts the creator.
- Event answer: new `suggestedAt` and `suggestions`.
