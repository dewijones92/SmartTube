# 0015. Batch 6: trace logging everywhere, and undo the pause after a panel closes

- Status: Accepted
- Date: 2026-11-13
- Decided by: Dewi ("preempt future stuff with MORE MORE LOGS", and go ahead with the panel fix); Claude (how
  each is built, and the guard's rules below, tightened after the fresh Sonnet review)

## Context
Backlog item 1: the video stops when the info or comments panel closes. Reproduced on the stick on 2026-11-13 at
22:58 (TV on, 32.74-focus.37, tennis video `cCQS2fUyDGk`, `FocusPanels` from ADR 0014). With the info panel open for
6 s, playback ran normally (position +500 ms a tick, `playWhenReady=true`). Within 500 ms of Back closing it,
`playWhenReady=false` with `loading=false` and `engineBlocked=false`, the position froze for the whole 8 s trace,
and the player was still paused a minute later. So it is a pause call, not a stall or a rebuffer. Upstream's close
hook (`BasePlayerController.mFitVideoFinish`) only restores zoom and gravity, so something else pauses, and the
release build's own logging (`Log`) is silent, so nothing on the device said what.

## Decision
- **`FocusTrace` logs the things a playback bug needs**, through `FocusLog` so release builds keep them:
  - every `playWhenReady` change, with the app frames that called it plus the top raw frames and the thread (ExoPlayer
    2.10 calls listeners synchronously, and the build is not obfuscated, so the stack names the caller; the raw frames
    keep a MediaSession or audio-focus pause visible, which sit in library packages);
  - a per-player id (`player@<hash>`) on every player line, so a replaced player shows as a new id;
  - playback state, `isPlaying`, suppression (audio focus), discontinuities, seeks with their caller, player errors;
  - every key reaching a `LeanbackActivity` (browse, search, player) or the panel window (`AppDialogActivity`), with
    action, repeat (every 10th repeat only), source and device; printing keys (letters, digits, punctuation, space) are logged as "(printing key)" so
    typed searches stay out of the log;
  - every activity lifecycle step (created, started, resumed, paused, stopped, destroyed) with task id.
  - every `PlaybackView.setPlayWhenReady` call with its caller, taken synchronously at the app's entry point, because a
    pause issued inside another player callback is queued by ExoPlayer and the listener's stack would not show it.
  Hooks: one line each in `PlaybackFragment.createPlayer`, `PlaybackFragment.setPlayWhenReady`,
  `LeanbackActivity.dispatchKeyEvent` and `AppDialogActivity.dispatchKeyEvent`; lifecycle comes from the callbacks `FocusSpeed` already registers.
- **The panel-close guard** (`FocusPanels`): if the video was playing when a panel closed and is paused within 3 s,
  Focus resumes it once and logs who paused it. It leaves the pause alone when: the user pressed a pause-type key after
  the close; no pause event happened after the close (a replaced or released player); the engine is
  blocked (background, PIP) or the player is not the resumed screen (Home was pressed), re-checked each tick until the
  window ends because both can be brief during the switch back from the panel; the caller shows a media key,
  the media session or audio focus (`MediaSession`, `PlayPause`, `AudioFocus`), a phone remote (`RemoteController`),
  the sleep timer, or a frame-rate switch (`AutoFrameRate`/`AfrRestart`, which resumes on its own); the video has
  ended; or it was already paused at the close. Each decision is logged with its reason. *Claude: a guard rather than removing
  the pause at its source, because the source is not yet known; the new caller log will name it, and the source fix
  (and an upstream PR) can follow.*

## Root cause (found with the new logs, 2026-11-13 23:32, 32.74-focus.40)
The info panel is its own window, shown at the TV's normal mode; the player's window asks for the video's frame
rate (50 Hz for the tennis clip). Closing the panel makes the system switch the HDMI mode back to 50 Hz
(`SetActiveConfig` at 23:32:06.746). The switch briefly drops HDMI audio (`hdmi_audio` switch state 0), Android
broadcasts `ACTION_AUDIO_BECOMING_NOISY` (06.874), and 0.6 s later a pause reaches the player through the media
session (`MediaSessionConnector` → upstream's `PlaybackFragment` control dispatcher → `dispatchSetPlayWhenReady`,
07.497). Upstream already blocks media-session play/pause for 5 s after an AFR switch for exactly this reason ("window
has different mode"), but a panel close is not an AFR switch, so its timestamp is old and the pause gets through.

## Amended 2026-11-14: the fork runs the upstream PR's exact code
Decided by Dewi ("any way we can do same code?") on Claude's recommendation. The fork cherry-picks the commit sent
upstream as yuliskov/SmartTube#6473: `PlaybackFragment` records when it resumes, and with AFR on its control dispatcher
ignores a media-session pause for 3 s after that. `FocusPanels.ignoreMediaSessionPause` is gone; the hook in that
dispatcher is now `FocusPanels.logMediaSessionRequest`, log-only, so the stick logs show each request and the PR's
check decides. When upstream merges #6473 the nightly sync absorbs the identical lines. The section below describes
the fork's first version (32.74-focus.41 to .43).

## Source fix (first version, superseded by the amendment above)
- One line at the top of upstream's control dispatcher: `FocusPanels.ignoreMediaSessionPause` ignores a media-session
  **pause** within 4 s of a panel closing when the user has pressed no pause-type key since (OK, Enter, space, gamepad
  A, headset hook, media play-pause, play, pause and stop; D-pad moves don't count, so a seek right after closing still
  keeps the guard). Plays always pass. Every media-session
  play/pause request is logged with the decision.
- `FocusTrace` also logs `AUDIO_BECOMING_NOISY`, HDMI audio plug changes and every display mode change, so the chain
  above shows up as named lines next time.
- The resume guard stays as a second line of defence.
- Upstream: a strong PR candidate. The general fix is to treat "the player window regained focus with a different
  display mode" like an AFR switch in that dispatcher; it needs the same evidence from an upstream build (rule 2).

## Consequences
- "Alexa, pause" within 4 s of closing a panel, with no remote key pressed, would be ignored once; the log says so.
- A Home press with a return inside 3 s of a panel closing could be resumed by the guard (third review's note).
- Logcat gets more lines per playback: a handful per play/pause and per key press. None fire per frame.
- If the guard's exclusions miss a deliberate pause (for example a pause from somewhere new within 3 s of closing a
  panel), Focus would resume it once; the log line says so, with the caller.
- Upstream verdict stays Pending until the caller is known and the same pause is seen on upstream SmartTube.

## Verified
On the stick, TV on, SmartTube player volume 0%, tennis video `cCQS2fUyDGk` (1080p50 AV1, AFR to 50 Hz), 2026-11-13:
- 32.74-focus.40, info panel: the logs named the pause (`PlaybackFragment$3.dispatchSetPlayWhenReady`, media session,
  856 ms after the close) next to the HDMI mode switch and `AUDIO_BECOMING_NOISY`; the guard rightly left a
  media-session pause alone.
- 32.74-focus.41, info panel (23:41:08): `media session asks playWhenReady=false: IGNORED` 607 ms after the close, with
  `AUDIO_BECOMING_NOISY` and the 59.94 -> 50 Hz display changes logged beside it; position kept climbing ~500 ms a
  tick for the whole 8 s trace (one slow tick while the TV re-synced); frames changing on screen.
- 32.74-focus.41, comments panel (23:42:20): same, ignored 451 ms after the close, playback continued.
- No `FATAL EXCEPTION` across the runs. First launch after `adb install -r` once took ~50 s to start playback (main
  thread busy, keys swallowed); warm starts were ~6 s including the 3 s AFR pause.
