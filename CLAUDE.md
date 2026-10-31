# SmartTube Focus (fork of yuliskov/SmartTube)

Dewi's fork. The `stfocus` flavour builds **SmartTube Focus** (`org.smarttube.focus`) for the Fire TV Stick 4K Max
in the living room. Upstream SmartTube is merged in every night, so keep our footprint small.

## Hard rules

1. **Every batch of changes gets a fresh Sonnet review before it ships.** After a coherent batch (a feature, a
   fix, a set of tweaks) and before pushing to `master` or building a release, spawn a NEW subagent with
   `model: sonnet` and `effort: high`, read-only, to review the diff. Give it the goal, the relevant ADRs in
   `docs/adr/`, and the decisions already made, so it does not flag agreed product choices as defects.
   Read its findings in full, fix what is real, and say what was skipped and why. A "new" Sonnet means a new
   agent, never a continued one: the point is a reviewer that has not seen the reasoning.
2. **Check every feature for upstream value.** When a feature lands, assess whether it would make sense as a
   pull request to `yuliskov/SmartTube`, and record the verdict in `docs/UPSTREAM-CANDIDATES.md`. Never open
   an upstream PR without Dewi's go.
3. **Record decisions as ADRs** in `docs/adr/` (next number, add to the README table, say who decided).
4. **Touch upstream files minimally.** Put logic in `common/.../misc/` (our classes) and keep upstream edits to
   one-line hooks. Gate Focus-only behaviour on `R.bool.focus_mode_available`, which is true only in
   `smarttubetv/src/stfocus/res/values/focus.xml`. Fewer upstream lines means fewer nightly merge conflicts.
5. **Match each file's line endings.** Most upstream Java is CRLF; `build.gradle` is LF. After any scripted
   edit, `git diff --shortstat` and `git diff --shortstat --ignore-all-space` must match.

## Layout

- `common/.../misc/FocusMode.java` launch prompt, session overlay, section filter (ADR 0003)
- `common/.../misc/FocusPrefetch.java` warm Subscriptions/Home while the prompt is up (ADR 0009)
- `common/.../misc/Moomoo.java` kids' section (ADR 0008)
- `common/.../misc/AfrRestart.java` "Adjusting TV…" + restart after a frame-rate switch (ADR 0009)
- `smarttubetv/src/stfocus/` flavour resources: icons, colours, update URL
- `focus/branding/*.py` regenerate icons
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
