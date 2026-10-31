# 0011. Speed batch 2: warm PO token, sticky refresh rate, section cache, next-episode preload

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi (approved batch 2 as proposed); Claude (implementation)

## Decision
All Focus-flavour only.
- **PO token warmed during the launch prompt.** `PoTokenGate`/`TvPoTokenProvider` are Kotlin-`internal` in the
  MediaServiceCore submodule (not forked), so no direct call. Instead, when the prefetched Subscriptions grid
  arrives, `FocusSpeed.prefetchFormatInfo` fetches video info for its first non-live video. That mints the TV PO
  token (cached until its expiry) and leaves the most likely first click in `FormatInfoWrapper`'s cache.
- **Sticky refresh rate.** Android picks the display mode from the top window's `preferredDisplayModeId`; leaving
  the player made the browse window (no preference) drop the TV back to 60 Hz, so each video cost two switches.
  `FocusSpeed` tracks the app's activities (lifecycle callbacks) and, when AFR switches mode
  (`AutoFrameRateController.onModeStart`), sets the same mode id on every other screen, including ones started
  later. Upstream's `ModeSyncManager` was meant for this but is never fed (`save(null)` only).
  While a sticky mode is active, `AutoFrameRateController.onInit` no longer re-captures the "original" mode
  (it would capture the previous video's rate), so restoring for Shorts/AFR-off still returns to the true base.
- **Section cache.** `FocusPrefetch` now wraps every grid/row section except History, Notifications, Settings,
  Playback queue, Blocked channels, Channels and Playlists: a loaded section is reused for 60 s (3 min for the
  launch prefetch) instead of refetched on every focus. Discarded on error, on an empty result, on data-source
  replacement (account change), on "No" (Shorts mask), and on `BrowsePresenter.refresh` ("Refresh section",
  3-hour Home refresh).
- **Next episode preload (Moomoo).** `SuggestionsController.onTickle` (once a minute) calls
  `Moomoo.preloadNextIfNeeded`: with under 2 minutes left in a Moomoo video that isn't last in its row, the next
  one's video info is fetched (the next video is only looked up once those checks pass).

## Consequences
- Coming back to the app from the launcher may switch the TV to the last video's rate (sticky until the process
  dies or another rate is used).
- Lists can be up to 60 s stale; an item removed via a menu action can reappear within that minute.
- The single-entry format cache means a preload evicts the current video's entry; a re-request of the current
  video (error recovery) refetches it.
