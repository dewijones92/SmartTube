# SmartTube Focus (fork of yuliskov/SmartTube)

Dewi's fork. The `stfocus` flavour builds **SmartTube Focus** (`org.smarttube.focus`) for the Fire TV Stick 4K Max
in the living room. Upstream SmartTube is merged in every night, so keep our footprint small.

## Hard rules

1. **Every batch of work ends with a review by a fresh Sonnet agent.** See "How a batch runs" below. A "fresh"
   Sonnet means a NEW subagent (`model: sonnet`, `effort: high`, read-only), never a continued one: the point is
   a reviewer that has not seen the reasoning. No batch reaches the TV without it.
2. **Check every feature for upstream value.** When a feature lands, assess whether it would make sense as a
   pull request to `yuliskov/SmartTube`, and record the verdict in `docs/UPSTREAM-CANDIDATES.md`. Never open
   an upstream PR without Dewi's go. An upstream PR (unless truly trivial) is only offered for a feature that has
   already passed rule 6 in this fork, and its description says so plainly: device, build, what happy path was
   run, what was checked (screen, logcat, timings). If the upstream branch's code differs from the fork's
   (no flavour gate, different call), test the exact upstream build too, or say it wasn't (Dewi, 2026-10-31).
   Actively try to upstream the sensible changes, and attach the end-to-end evidence to the PR itself: screenshots
   or a screen recording of the happy path, and the relevant logcat lines, from the build the PR describes
   (Dewi, 2026-11-13). A feature with no attachable evidence isn't ready to offer upstream.
   Measure the change **on its own** (A/B against upstream, same videos, 5 runs each) before claiming a number:
   PR #6394 claimed a saving that came from other changes and measured as zero, so we closed it.
3. **Record decisions as ADRs** in `docs/adr/` (next number, add to the README table, say who decided).
4. **Touch upstream files minimally.** Put logic in `common/.../misc/` (our classes) and keep upstream edits to
   one-line hooks. Gate Focus-only behaviour on `R.bool.focus_mode_available`, which is true only in
   `smarttubetv/src/stfocus/res/values/focus.xml`. Fewer upstream lines means fewer nightly merge conflicts.
5. **Match each file's line endings.** Most upstream Java is CRLF; `build.gradle` is LF. After any scripted
   edit, `git diff --shortstat` and `git diff --shortstat --ignore-all-space` must match.
6. **Every non-trivial change is tested end to end on the TV before Dewi gets it** (Dewi, 2026-10-31: *"so I
   don't get any nasty surprises when I use it"*). At least one real happy path of the changed feature, driven
   on the Fire Stick with the build that will ship: open the app, do what Dewi would do, and check the result on
   screen (screenshot) and in the device state or logcat, not just that it compiled or that a reviewer approved.
   Also check logcat for `FATAL EXCEPTION` across the run. "Trivial" means docs, strings, or a change with no
   runtime path; when unsure, it isn't trivial. Why: on 2026-10-31 two code reviews approved a batch that crashed
   on the first TV run.
7. **Update the ADRs in the same change as the code** (from Totum). A new decision gets an ADR; a changed one is
   edited; a reversed one is marked superseded and points at its replacement. Read an area's ADRs before changing it.
8. **Check every way in.** A change to the launch, prompt or landing is checked on each path that reaches it: a
   normal launch, a launch from a link or cast (no prompt), Back or Home on the prompt, the update pop-up replacing
   the prompt, No, Kids, and a relaunch. Check the siblings of what you touch before calling it done.
9. **Look for code to reuse before building**, and name it in the plan. Upstream usually has the piece already
   (the dialog window, the search page, `SearchTagStorage`); a second copy is a merge conflict waiting to happen.
10. **Every change must be provable from the device's logs.** Log each decision a user would notice, and why, with
    the values it decided from. Our classes log through `FocusLog` (Android's log), never SmartTube's `Log`, which
    is silent in release builds. Ask: if this misbehaves on the TV, which logcat line tells me?

## How a batch runs (Dewi, 2026-10-31 and 2026-11-06; modelled on Totum's)

**Big batches, one build** (Dewi, 2026-11-06): we don't build locally. Write
the whole batch, push it to a branch, let CI build it once, then test everything against that one APK. Fix what
testing finds in one more push, not a build per change. Watch CI on the exact commit to its end.

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
4. **Test end to end on an emulator, or on the Fire Stick with the TV OFF, then ship** (Dewi, 2026-11-06: never
   switch the TV on). The emulator is `smarttube-api35` (API 35 phone image at 1920x1080, landscape, no audio; it is
   not signed in, so signed-in sections show their sign-in prompt). The stick can be driven over adb while the TV is
   off, but do not send `KEYCODE_WAKEUP` or anything that wakes the TV through HDMI-CEC; if a step needs the screen
   on, use the emulator. The older notes below about switching the TV on apply only when Dewi lends the TV.
   **Original step 4 (TV lent):** Build the batch on a branch (non-`master` builds don't publish),
   `gh run download` the APK, `adb install -r` it, and run each changed feature's happy path on the TV (rule 6).
   Only then merge to `master` and publish. The TV is Dewi's to lend: switch it on with `KEYCODE_WAKEUP` when
   needed; afterwards sleep the stick and ask Dewi to switch the TV off with the remote (no adb route found:
   `KEYCODE_SLEEP`, `KEYCODE_TV_POWER` and CEC auto-off all failed to send a standby on 2026-10-31).
   **Before the first playback: set the app's own player volume to 0%** (Settings › Player › Volume) and confirm it
   by screenshot; restore it after the last playback. Forgotten once (2026-10-31, batch 3) and the TV played aloud.
   **Before every OK that could start playback, check the focused tile's title by screenshot.**
   **Test with tennis videos, never kids' content** (Peppa, Ms Rachel, Moomoo rows): Dewi's son gets distracted
   (Dewi, 2026-10-31). Deep-link tennis video IDs; for Moomoo itself, check the rows render without playing.
5. **Report** in the end-of-run shape: 🚧 Needs you, Changed, 🔍 Found, ⚠️ Couldn't confirm.

## Layout

- `common/.../misc/FocusMode.java` launch prompt, session overlay, section filter (ADR 0003)
- `common/.../misc/FocusPrefetch.java` warm Subscriptions/Home while the prompt is up, hidden by a solid prompt
  background (ADR 0009, 0013); the focus-mode Search page and Kids History live in `FocusSections`/`FocusMode` (ADR 0014)
- `FocusMode.plainSearchTags` empty search box shows only your own saved searches in focus mode (ADR 0013)
- `common/.../misc/FocusLog.java` logging that survives release builds
- `common/.../misc/Moomoo.java` kids' section (ADR 0008)
- `common/.../misc/AfrRestart.java` "Adjusting TV…" + restart after a frame-rate switch (ADR 0009)
- `docs/BACKLOG.md` work Dewi has asked for, in order; no code for an item until he says go
- `common/.../misc/FocusPanels.java` logs playback for 8 s around an info/comments panel (ADR 0014)
- `focus/bridge/` separate Alexa voice-search bridge app, `com.amazon.firetv.youtube` -> Focus (ADR 0014); built by
  `./gradlew -p focus/bridge assembleRelease`
- `smarttubetv/src/stfocus/` flavour resources: icons, colours, update URL
- `focus/branding/*.py` regenerate icons
- `focus/tools/measure-start.sh` click-to-picture timings from logcat (navigates Moomoo rows: prefer deep-linking tennis videos instead, see step 4)
- `.github/workflows/nightly-sync.yml` cron 03:00 UTC (GitHub actually starts it around 10:00 UTC): merge upstream,
  build, sign, publish release `latest`. A failed push is retried once after 30 s before it raises an issue.

## Build, release, device

- No local builds, by choice (Dewi, 2026-11-06): batch the work (see above) and build in CI. (A JDK 17 does exist at
  `/home/dewi/code/jdk/jdk-17.0.13+11`; the old "JDK 21 only" note was wrong.) Build in CI:
  `gh workflow run nightly-sync.yml -R dewijones92/SmartTube --ref master -f force_build=true`.
- `gh run view --log` can come back EMPTY for a finished run; `gh api repos/dewijones92/SmartTube/actions/runs/<id>/logs`
  (a zip) has the full log. An empty capture is not a clean run.
- A doc's status line is a claim: when you touch an area, check it against the code and correct it.
- Push over SSH (`origin` is SSH). The gh token lacks the `workflow` scope, so HTTPS pushes of workflow files fail.
- The TV updates through the app's own updater: "Install update", then Android's Install. For a quick test,
  `adb -s 192.168.0.211:5555 install -r <apk>` works because the same key signs every build.
- The signing key is `~/.credentials/smarttube-focus.jks`. Losing it breaks updates for the installed app.
- Our classes log through `FocusLog` (Android's `Log.i`), so `adb logcat | grep dewidebug` works on release builds.
  Upstream classes still use SmartTube's `Log`, which is silent in release builds.
- Never play a video on the TV with sound; see the firetv-adb-control skill and the smarttube memory notes.
