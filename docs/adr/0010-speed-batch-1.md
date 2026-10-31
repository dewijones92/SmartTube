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
- Device setting: AFR pause 4.0 s -> 3.0 s.

## Measured on the TV (Fire TV Stick 4K Max, 31 Oct 2026, click -> AFR switch complete)
| | before (.8) | after (.9) |
|---|---|---|
| cold first video | +8.5 s | +7.5 s (PO-token WebView dominates: decoder at +6.1 s) |
| warm video | +6.0 s | +2.2 / +3.5 / +3.1 s (3 runs) |
Picture follows after the AFR pause (now 3 s). Next target: mint the PO token during the launch prompt.

## Consequences
- One extra format request when the player's request misses the cache: live/upcoming streams, a player-JS warm-up
  that failed (offline), or a client switch mid-load. A clicked-then-abandoned video is always fetched.
- 4K switches may show the raw mode change instead of Amazon's overlay.
- Upstream candidates: format-fetch-on-click and warm-engine (Yes), overlay skip (Maybe; device-specific).

## Correction 2026-10-31 (A/B on the TV)
Format-fetch-on-click on its own measured **no gain**: upstream `4b3f333e` vs the same change as a clean upstream PR,
5 identical Tennis TV deep links each, decoder start median 2.46 s vs 2.58 s. The player engine starts ~0.2 s after
the click on this device, so there is little to overlap. The batch-1 gains above came from the engine warm-up and
the removed AFR overlay wait. `FocusSpeed.prefetchFormatInfo` stays: it is harmless and is reused by the launch
prefetch (ADR 0011) and the Moomoo preload, where the fetch is genuinely ahead of time.
