# 0003. Launch prompt; focus mode is a runtime overlay

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi (prompt on every launch, Yes focused; No = original SmartTube for that session; what to
  hide: Home recommendations, related videos/autoplay, Shorts; Subscriptions stays);
  Claude (overlay design, exact section list, playback mode), accepted in the plan

## Decision
`common/.../misc/FocusMode.java` holds an in-process flag. `SplashPresenter` calls
`FocusMode.onAppLaunch` on a normal launch (skipped when the launch intent carries a link or cast). Focus
mode is on by default before the user answers, so Back or a replaced dialog still lands in focus mode.

One-line hooks read the flag instead of rewriting settings:
- `SidebarService.getPinnedItems` filters out Home, Recommended, Shorts, Trending, Kids, Sports, Live,
  Gaming, News, Music, Movies.
- `SidebarService.getBootSectionId` returns Subscriptions.
- `PlayerTweaksData.isSuggestionsDisabled` returns true.
- `PlayerData.getPlaybackMode` returns `PLAYBACK_MODE_LIST` (play only explicit playlists) —
  *Claude, not separately asked*; Dewi's original beta setting was "stop after one video".
- Shorts hiding lives in the MediaServiceCore submodule, so FocusMode saves the user's Shorts bitmask,
  sets "hide everywhere" on Yes, and restores the saved mask on No.

## Consequences
- "No" is simply the stored settings; nothing needs undoing.
- If the update pop-up (10 s after launch) arrives before the prompt is answered, it replaces the prompt
  and the session stays in focus mode.
- Choosing No keeps the current section (Subscriptions) rather than jumping to Home.
- The launch intent must be read at the start of `runPerViewTasks`: after `applyNewIntent` the splash
  view is gone (this crashed build .1).
