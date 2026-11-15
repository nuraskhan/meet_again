# Plan: a noticeable "Try demo" with a choice of account, and the demo video

## What is asked

1. "Try demo" on the welcome page must be noticed.
2. A click on it opens a drop-down: sign in as Steve Jobs or as Steve Wozniak.
3. The video https://www.youtube.com/watch?v=LBUlGUmj4EQ is shown on the welcome page and in the
   README, with a player.

## Decisions

- **One component, `DemoMenu.vue`**, used in the top bar and under the video. The button carries the
  accent and a slow pulse ring; the pulse stops once the menu has been opened and is absent for
  people who asked for reduced motion. The menu items are the existing `/fl/steve-jobs` and
  `/fl/steve-wozniak` links, so nothing changes on the server.
- **The menu stays on the screen**: after opening it is measured and moved sideways if it would run
  over the edge (the button stands in the middle of the bar on a phone).
- **`DemoVideo.vue`: the player is loaded only on play.** Until then the page shows its own poster
  and nothing is requested from YouTube; on play the frame comes from `youtube-nocookie.com`.
- **The video section stands right after the first screen**, before "How it works".
- **README**: GitHub removes frames from a README, so a player cannot be placed there. The nearest
  thing is the video's picture as a link that opens the player on YouTube. Added to both READMEs
  (server and web application) under "Try it".

## Checks

- `npm run build`.
- In the browser: the menu opens and closes (outside click, Escape), both items sign in, the video
  plays in place, a 390 px wide page has no sideways scrolling and the menu fits.
