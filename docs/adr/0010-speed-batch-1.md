# 0010. Speed batch 1: earlier format fetch, warm engine, no AFR overlay wait

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi (make the app snappier; approved batch 1; keep 4K quality); Claude (selection of changes from a
  Sonnet code investigation, spot-checked)

## Decision
`common/.../misc/FocusSpeed.java`, Focus flavour only:
- **Format fetch on click.** `PlaybackPresenter.openVideo` starts `getFormatInfoObserve(videoId)` before the player
  screen opens. `FormatInfoWrapper` keeps a one-entry cache behind a lock, so the player's own request reuses it.
- **Warm engine at launch.** `FocusMode.onAppLaunch` calls `refreshCacheIfNeeded()` (async) instead of waiting for
  upstream's 10 s `APP_INIT_DELAY_MS`.
- **No AFR overlay wait.** `DisplaySyncHelper` passes `allowOverlayDisplay = false` (both the switch and the restore on exit), so `UhdHelper` no longer waits
  ~2 s for an Amazon interstitial broadcast that never arrives on this device before switching to a 4K mode.
- Device setting: AFR pause trimmed to fit the shorter switch (measured on the TV).

## Consequences
- One extra format request when the player's request misses the cache: live/upcoming streams, a player-JS warm-up
  that failed (offline), or a client switch mid-load. A clicked-then-abandoned video is always fetched.
- 4K switches may show the raw mode change instead of Amazon's overlay.
- Upstream candidates: format-fetch-on-click and warm-engine (Yes), overlay skip (Maybe; device-specific).
