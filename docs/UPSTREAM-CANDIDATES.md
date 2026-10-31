# Upstream pull-request candidates

Each feature of the fork, judged for a pull request to `yuliskov/SmartTube`. Nothing is sent without Dewi's go.
Verdicts: **Yes** (worth offering), **Maybe** (needs generalising first), **No** (personal to this fork).

| Feature | ADR | Verdict | Why / what it would need |
|---|---|---|---|
| Restart from the opening point after an Auto Frame Rate switch, with an "Adjusting TV to N Hz…" message | 0009 | **Yes** | Generally useful: AFR users see a black screen and lose the first seconds. Would need to become an option under Settings › Auto Frame Rate, use a string resource, and drop the `focus_mode_available` gate. Small change in `AutoFrameRateController`. |
| Prefetch a section's content while a boot dialog is shown | 0009 | **Maybe** | Upstream has no launch prompt; the reusable part is "warm the boot section early". Would need a measured gain on plain launches first. |
| Distraction-free launch prompt and session overlay | 0003 | **Maybe** | Upstream already has Child mode and section toggles. Could be offered as an opt-in "Ask on launch: distraction-free?" setting, but it's a product decision for upstream; ask the maintainer in an issue first. |
| Start the format request on click, before the player screen opens | 0010 | **Yes** | One line in `PlaybackPresenter`, reuses the existing `FormatInfoWrapper` cache; drop the flavour gate. Needs a before/after timing to convince the maintainer. |
| Warm the player engine at launch instead of after 10 s | 0010 | **Yes** | Async call already used by upstream, just earlier. |
| Skip the Amazon interstitial wait before 4K mode switches | 0010 | **Maybe** | Saves ~2 s on Fire OS 8 where the broadcast never comes; may matter on other Amazon devices. Offer as an option. |
| Keep the AFR refresh rate on other screens after leaving the player (no switch back and forth between videos) | 0011 | **Yes** | Finishes what upstream's unused `ModeSyncManager` set out to do. Would want an option (some users want the UI back at 60 Hz). |
| 60 s section cache instead of refetching on every sidebar focus | 0011 | **Maybe** | Big feel improvement; needs care with menu actions and a setting. |
| Preload the next video's info near the end | 0011 | **Maybe** | Upstream has a dead `preloadNextVideoIfNeeded`; ours is scoped to Moomoo rows. Generalised, it'd be a clean PR. |
| Moomoo kids' section with fixed channels | 0008 | **No** | Hard-coded to our family's channels. A generic "pin a channel's Popular videos as a section" might be Maybe. |
| Separate flavour, jade branding, nightly sync, own update URL | 0002, 0004–0006 | **No** | Fork plumbing. |
