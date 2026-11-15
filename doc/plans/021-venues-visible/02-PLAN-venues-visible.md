# PLAN 021: venues that can actually be seen

Why "Choose a place" and "Suggest places" showed nothing, and what is done about each cause.

| Cause | What is done |
|-------|--------------|
| The live database has no venues: the import could not reach it from the developer's machine. | The import becomes a step of the release on Heroku. It runs only if the application has the config var `HANGELO_2GIS_KEY`, and only while there are no venues from 2GIS yet. A failed import does not stop a release. |
| "Suggest places" looked only inside the travel radius. Someone who never said how far they go gets the invitation's own distance (often 2-3 miles), and nothing of the right kind may be that near. | If nothing is within anyone's radius, the nearest places within 30 miles are offered, each marked as beyond someone's reach. |
| The list of venues said nothing about how far each is. | `POST /place/search` takes the caller's position: every venue reports its distance and the nearest come first. The picker shows name, rating and distance; it measures from the location saved for the plan, or from the device when the person asks. |

The position travels in the body of the request, never in the address.
