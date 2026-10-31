# Upstream pull-request candidates

Each feature of the fork, judged for a pull request to `yuliskov/SmartTube`. Nothing is sent without Dewi's go.
Verdicts: **Yes** (worth offering), **Maybe** (needs generalising first), **No** (personal to this fork).

| Feature | ADR | Verdict | Why / what it would need |
|---|---|---|---|
| Restart from the opening point after an Auto Frame Rate switch, with an "Adjusting TV to N Hz…" message | 0009 | **Yes** | Generally useful: AFR users see a black screen and lose the first seconds. Would need to become an option under Settings › Auto Frame Rate, use a string resource, and drop the `focus_mode_available` gate. Small change in `AutoFrameRateController`. |
| Prefetch a section's content while a boot dialog is shown | 0009 | **Maybe** | Upstream has no launch prompt; the reusable part is "warm the boot section early". Would need a measured gain on plain launches first. |
| Distraction-free launch prompt and session overlay | 0003 | **Maybe** | Upstream already has Child mode and section toggles. Could be offered as an opt-in "Ask on launch: distraction-free?" setting, but it's a product decision for upstream; ask the maintainer in an issue first. |
| Moomoo kids' section with fixed channels | 0008 | **No** | Hard-coded to our family's channels. A generic "pin a channel's Popular videos as a section" might be Maybe. |
| Separate flavour, jade branding, nightly sync, own update URL | 0002, 0004–0006 | **No** | Fork plumbing. |
