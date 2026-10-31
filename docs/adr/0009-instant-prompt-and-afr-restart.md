# 0009. Instant start after the prompt; restart from the opening point after a frame-rate switch

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi (choosing at the prompt should land instantly with no spinner; keep Auto Frame Rate; after
  the TV switches mode the video should play from where it was opened, not pause half-way; show an
  "Adjusting…" message); Claude (implementation, cache lifetime, 4 s pause)

## Decision
**Prefetch.** `FocusPrefetch` wraps the Subscriptions grid and Home rows data sources (registered via
`FocusMode.registerSections`, which replaces the earlier one-line Moomoo hook in `BrowsePresenter`). When the
launch prompt appears, both start loading; the first consumer within 3 minutes gets the cached result, later
loads fetch fresh as before. Choosing No now jumps to Home (`BrowsePresenter.selectSection`).

**Frame-rate switch.** Diagnosis on the device: the blank screen is Auto Frame Rate changing the refresh rate
(60/50/30 Hz history in SurfaceFlinger; all layers SDR), not HDR. Android broadcasts
`ACTION_AUDIO_BECOMING_NOISY` during the HDMI re-sync. Device setting "Auto Frame Rate (pause when
switching)" set from Never to **4.0 s** (blocks playback at load, resumes after the delay). `AfrRestart`
(called from `AutoFrameRateController`) shows "Adjusting TV to N Hz…", records the position when the switch
starts, and on resume seeks back to it and plays — only when the AFR pause setting is above 0 (with Never, upstream
behaviour is unchanged). The play/pause state after resume follows the controller's saved state.

## Consequences
- A freshly opened video starts at 0:00 after the switch; a resumed one starts at its saved position.
- Up to 4 s of black/pause on a switch, by design.
- Home/Subscriptions shown straight after launch may be up to 3 minutes old.
