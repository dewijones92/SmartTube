# SmartTube Focus (fork of yuliskov/SmartTube)

Dewi's fork. The `stfocus` flavour builds **SmartTube Focus** (`org.smarttube.focus`) for the Fire TV Stick 4K Max
in the living room. Upstream SmartTube is merged in every night, so keep our footprint small.

## Hard rules

1. **Every batch of work ends with a review by a fresh Sonnet agent.** See "How a batch runs" below. A "fresh"
   Sonnet means a NEW subagent (`model: sonnet`, `effort: high`, read-only), never a continued one: the point is
   a reviewer that has not seen the reasoning. No batch reaches the TV without it.
2. **Check every feature for upstream value.** When a feature lands, assess whether it would make sense as a
   pull request to `yuliskov/SmartTube`, and record the verdict in `docs/UPSTREAM-CANDIDATES.md`. Never open
   an upstream PR without Dewi's go.
3. **Record decisions as ADRs** in `docs/adr/` (next number, add to the README table, say who decided).
4. **Touch upstream files minimally.** Put logic in `common/.../misc/` (our classes) and keep upstream edits to
   one-line hooks. Gate Focus-only behaviour on `R.bool.focus_mode_available`, which is true only in
   `smarttubetv/src/stfocus/res/values/focus.xml`. Fewer upstream lines means fewer nightly merge conflicts.
5. **Match each file's line endings.** Most upstream Java is CRLF; `build.gradle` is LF. After any scripted
   edit, `git diff --shortstat` and `git diff --shortstat --ignore-all-space` must match.

## How a batch runs (Dewi, 2026-10-31; modelled on Totum's)

1. **Start with a discussion.** Before a batch, list what it will contain and ask Dewi only what he can answer
   (scope, defaults, trade-offs). Put the plan on a page when it changes code; write each decision into an ADR so
   it survives a context reset. Questions are for intent and defaults, never for facts the code can answer.
2. **Build the features.** Small commits, upstream files touched by one-line hooks, Focus-only behaviour gated,
   ADRs and `docs/UPSTREAM-CANDIDATES.md` updated in the same change.
3. **End the batch with a fresh Sonnet review and a before/after check, in parallel.**
   - The review: a new Sonnet `reviewer` agent on the batch's commits (`git show <sha>` or a range), given Dewi's
     requests, the ADRs, and every decision already accepted, so agreed product choices aren't reported as defects.
     Read the whole report, never a truncated one. Check each finding against the code before acting, because a
     reviewer states wrong claims as confidently as right ones. Fix what holds up, and say what was skipped and why.
     A fix that is more than a mechanical follow-up gets its own fresh review.
   - The before/after check, for anything touching playback, loading or startup: measure on the TV with
     `focus/tools/measure-start.sh` against the build installed before the batch, cold and at least three warm runs
     (one run is noise). Set SmartTube's own player volume to 0% first (Settings › Player › Volume) and restore it
     after: the system mute key does not reliably mute playback on this stick. Record the numbers in the ADR.
4. **Then ship and report.** Push, build, install (or let the in-app updater do it), check the changed screen on
   the TV, and report in the end-of-run shape: 🚧 Needs you, Changed, 🔍 Found, ⚠️ Couldn't confirm.

## Layout

- `common/.../misc/FocusMode.java` launch prompt, session overlay, section filter (ADR 0003)
- `common/.../misc/FocusPrefetch.java` warm Subscriptions/Home while the prompt is up (ADR 0009)
- `common/.../misc/Moomoo.java` kids' section (ADR 0008)
- `common/.../misc/AfrRestart.java` "Adjusting TV…" + restart after a frame-rate switch (ADR 0009)
- `smarttubetv/src/stfocus/` flavour resources: icons, colours, update URL
- `focus/branding/*.py` regenerate icons
- `focus/tools/measure-start.sh` click-to-picture timings from logcat (cold/warm, Moomoo rows)
- `.github/workflows/nightly-sync.yml` 03:00 UTC: merge upstream, build, sign, publish release `latest`

## Build, release, device

- There is no local build (JDK 21 only here; Gradle 7.5 needs 17). Build in CI:
  `gh workflow run nightly-sync.yml -R dewijones92/SmartTube --ref master -f force_build=true`.
- Push over SSH (`origin` is SSH). The gh token lacks the `workflow` scope, so HTTPS pushes of workflow files fail.
- The TV updates through the app's own updater: "Install update", then Android's Install. For a quick test,
  `adb -s 192.168.0.211:5555 install -r <apk>` works because the same key signs every build.
- The signing key is `~/.credentials/smarttube-focus.jks`. Losing it breaks updates for the installed app.
- `dewidebug` log lines go through SmartTube's `Log`, which is silent in release builds. Do not rely on them in logcat.
- Never play a video on the TV with sound; see the firetv-adb-control skill and the smarttube memory notes.
