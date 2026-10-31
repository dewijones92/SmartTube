# Upstream pull-request candidates

Each feature of the fork, judged for a pull request to `yuliskov/SmartTube`. Nothing is sent without Dewi's go.
Verdicts: **Yes** (worth offering), **Maybe** (needs generalising first), **No** (personal to this fork).

| Feature | ADR | Verdict | Why / what it would need |
|---|---|---|---|
| Restart from the opening point after an Auto Frame Rate switch, with an "Adjusting TV to N Hz…" message | 0009 | **No** (was Yes) | Reviewed 2026-10-31: with upstream's own "pause when switching" set, playback is already blocked during the switch, so the rewind is a no-op there; our real fix was that setting. The toast exists upstream, commented out because Ugoos shows its own. Draft kept on branch `pr/afr-resume-from-start`, not sent. |
| Prefetch a section's content while a boot dialog is shown | 0009 | **Maybe** | Upstream has no launch prompt; the reusable part is "warm the boot section early". Would need a measured gain on plain launches first. |
| Distraction-free launch prompt and session overlay | 0003 | **Maybe** | Upstream already has Child mode and section toggles. Could be offered as an opt-in "Ask on launch: distraction-free?" setting, but it's a product decision for upstream; ask the maintainer in an issue first. |
| Start the format request on click, before the player screen opens | 0010 | **No — PR #6394 closed by us** | A/B on the TV (upstream CI builds, 5 tennis deep links each): median 2.46 s → 2.58 s, i.e. no gain; engine starts ~0.2 s after click. Results posted on the PR. |
| Warm the player engine at launch instead of after 10 s | 0010 | **Yes** | Async call already used by upstream, just earlier. |
| Skip the Amazon interstitial wait before 4K mode switches | 0010 | **Maybe** | Saves ~2 s on Fire OS 8 where the broadcast never comes; may matter on other Amazon devices. Offer as an option. |
| Keep the AFR refresh rate on other screens after leaving the player (no switch back and forth between videos) | 0011 | **Issue opened: yuliskov/SmartTube#6395** (resubmits stale #198) | Draft on `pr/afr-keep-mode` (opt-in setting, ~110 lines) has 4 review findings to fix, and upstream reverted #6143 (writing `preferredDisplayModeId = 0`) on 2026-10-06 pending investigation. Ask the maintainer in an issue, with our TV measurements, before a PR. |
| 60 s section cache instead of refetching on every sidebar focus | 0011 | **Maybe** | Big feel improvement; needs care with menu actions and a setting. |
| Preload the next video's info near the end | 0011 | **Maybe** | Upstream has a dead `preloadNextVideoIfNeeded`; ours is scoped to Moomoo rows. Generalised, it'd be a clean PR. |
| Kids mode / bedtime timer / Tennis section | 0012 | **No** | Built on our curated sections and launch prompt; upstream has Child mode. A generic "stop autoplay after N minutes" setting might be Maybe later. |
| Continue watching (History filtered to part-watched) | 0012 | **Maybe** | Small and general; would need to be a normal section toggle upstream. |
| Moomoo kids' section with fixed channels | 0008 | **No** | Hard-coded to our family's channels. A generic "pin a channel's Popular videos as a section" might be Maybe. |
| Separate flavour, jade branding, nightly sync, own update URL | 0002, 0004–0006 | **No** | Fork plumbing. |
