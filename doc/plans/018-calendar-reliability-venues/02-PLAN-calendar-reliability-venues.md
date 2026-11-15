# PLAN 018: calendar of availability, reliability rating, venues, default pictures

## 1. Calendar and availability (requests 1 and 3)

- Table `availability`: a person marks days as BUSY, FREE, or TRIP (away in a city), with an
  optional note. Whole days as plain dates - no time zone.
- Friends see the kind, the days and the city, never the note; strangers see nothing.
- One calendar shows everything: the person's plans, their own marks, and that friends have marks
  on a day. Busy and free stretches are bands across the days.
- The calendar stands at the right of the feed on a wide screen, and is a page of its own
  (`/calendar`) otherwise.
- The new-plan form says which invited friends are busy or away on a proposed date.

## 2. Reliability rating (request 2)

- A promise: saying "coming" to an event, or creating one.
- Kept: the event took place and the person was in (and was not marked absent); the creator held
  their event.
- Broken: left after saying "coming"; marked by the creator, after the event, as not having come;
  the creator cancelled an event somebody was coming to.
- Score = kept / (kept + broken), 0 to 100; shown once there are three promises. Counted from the
  history each time, not stored.
- Below 50: the "Air" sticker, saying how much air (100 minus the score).
- Shown: the person's own on the feed and in the profile; anyone's on their page; the inviter's
  on a feed card.

## 3. Venues (request 4)

- The existing table `place` is the venue. It gains the city, the source and the id at the
  source, the number of reviews; prices and rating may be unknown (empty).
- An event gets `place_id`: the creator attaches a venue, changes it, takes it off.
- Looking through venues: by category, city, a part of the name.

## 4. Loading venues (request 5)

- A tool, `place-import` (`make places`): the 2GIS Places API, 20 venues per category for Astana
  and Almaty - those the most reviews stand behind. The key is read from `HANGELO_2GIS_KEY`.
  Running again updates, never doubles.
- Not taken: photographs (they belong to their authors), phones and sites (need an extra
  permission of the key), prices (the directory has none).

## 5. Default pictures (request 6)

- One drawn scene per activity, made for this application. A plan shows the scene of its
  activity. Venues have no photographs, so for now it is always this picture.

Migrations: `V015__calendar_reliability_venue.sql`, `V016__place_unknowns.sql`.
