# Report: a noticeable "Try demo" with a choice of account, and the demo video

## Done

Web application (`meet_again_ui`):

- `src/components/DemoMenu.vue` (new): the accent "Try demo" button with a pulse ring and a
  drop-down "Sign in as": Steve Jobs ("Organises the plans"), Steve Wozniak ("Gets invited"). Closes
  on an outside click and on Escape; moves itself back onto the screen when it would run over the
  edge. Test ids: `demo-menu-button`, `demo-menu`, `demo-menu-steve-jobs`, `demo-menu-steve-wozniak`.
- `src/components/DemoVideo.vue` (new): a 16:9 poster with a play button; the YouTube player
  (`youtube-nocookie.com`) is loaded on play. A link "Open on YouTube" below. Test ids: `demo-video`,
  `demo-video-play`, `demo-video-link`.
- `src/views/WelcomeView.vue`: the top bar's "Try demo" link is replaced by the menu; a new section
  "See Hangelo in action" with the video and a second "Try demo" stands before "How it works".
- `README.md`: section "Watch the demo" with the video's picture linking to YouTube.

Server repository:

- `README.md`: the same "Watch the demo" section.

## Checked

- `npm run build` passes.
- In Chrome against the local stack: the menu opens with both accounts; an outside click closes it;
  choosing Steve Wozniak signs in and opens `/plans`; the video plays in place after a press on the
  poster, and no frame exists before it.
- At 390 px: no sideways scrolling; the menu, first measured at 155-415 px (over the edge), now
  stands at 100-360 px.

## Not done, and why

- A real player inside the README: GitHub does not render frames there. The picture-link is the
  closest thing available.
- The poster is the page's own drawing, not a frame of the video: taking YouTube's picture would
  contact YouTube before the visitor chose to watch.

## Left to the human

- Commit, push, deploy.
