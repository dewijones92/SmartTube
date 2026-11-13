# 0013. Hidden prefetch behind the prompt; distraction-free opens a plain search page

- Status: Accepted
- Date: 2026-11-06
- Decided by: Dewi (keep prefetching but show nothing until a choice is made; distraction-free must not open on
  Subscriptions; open SmartTube's existing search page, YouTube's suggestions hidden, own recent searches kept;
  test on an emulator, or the Fire Stick with the TV off); Claude (solid prompt background, Channels as the section
  behind the search page, Back on the prompt stays on that section, Focus logs through Android's log)
- Supersedes: the "boot to Subscriptions" part of [ADR 0003](0003-launch-prompt-runtime-overlay.md).
  Amends [ADR 0009](0009-instant-prompt-and-afr-restart.md): the prefetch is unchanged but no longer visible.

## Context
The prompt is a see-through dialog, so Subscriptions loaded and showed behind it before anything was chosen, and
"Yes" landed on Subscriptions: a wall of new videos. The original brief (29 Oct) was "just search". SmartTube's
search page, with an empty box, shows YouTube's popular searches for the country, and falls back to the user's own
saved searches (`SearchTagStorage`, 50 most recent) only when YouTube returns nothing.

## Decision
- **Hidden prefetch.** While the prompt is up (`FocusMode.sPromptShowing`), `AppDialogActivity.onResume` calls
  `FocusMode.applyPromptBackground`, which gives the dialog window a solid black background. The window stays
  translucent, so the browse screen underneath keeps loading; `FocusPrefetch` is unchanged. The flag is cleared when
  the prompt closes, and also from `AppDialogActivity.onDestroy`, because a system destroy drops the close callback
  without running it and a stuck flag would turn every later dialog black.
- **Yes** closes the prompt and opens the search page (`SearchPresenter.startSearch(null)`). With SmartTube's
  "Instant voice search" setting on, that opens voice search instead of the keyboard, as upstream does.
- **Behind the search page** the start section is Channels (`bootSectionId` returns `TYPE_CHANNEL_UPLOADS` in focus
  mode), so Back from search shows Channels, not Subscriptions. *Claude, not separately asked.* *Amended by [ADR 0014](0014-batch5-search-page-voice-bridge.md): a Search page, not Channels.*
- **Plain search.** In focus mode (Kids included), an empty box shows only the user's own saved searches, never
  YouTube's suggestions (`FocusMode.plainSearchTags`, hooked in `MediaServiceSearchTagProvider`). Typing still gets
  YouTube's autocomplete. Full SmartTube mode is unchanged.
- **Back, Home, or another dialog replacing the prompt** stays in focus mode on Channels and does not open search:
  the prompt's close callback also runs when Home is pressed, and opening search from there would pull the app back
  in front of the launcher. *Claude, not separately asked.*
- **Logs.** Our classes log through `FocusLog` (Android's `Log.i`/`Log.e`), because SmartTube's own `Log` is
  silent in release builds; every choice and landing is a `dewidebug` line in logcat.

## Consequences
- No and Kids behave as before: No discards the prefetch (its Shorts setting changes) and loads Home; Kids loads
  Moomoo, which was never prefetched.
- The Subscriptions prefetch no longer feeds the landing screen. It is kept because its first video's format fetch
  warms the PO token (ADR 0011), which the first playback from search also needs, and a Subscriptions visit within
  3 minutes is served from it.
- Channels, behind the search page, is fetched fresh at launch (it is uncached, like History).
- An update pop-up that replaces the prompt also gets the black background until it closes.
