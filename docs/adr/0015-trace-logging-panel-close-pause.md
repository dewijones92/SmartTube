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
    action, repeat (every 10th repeat only), source and device; letters and digits are logged as "(printing key)" so
    typed searches stay out of the log;
  - every activity lifecycle step (created, started, resumed, paused, stopped, destroyed) with task id.
  - every `PlaybackView.setPlayWhenReady` call with its caller, taken synchronously at the app's entry point, because a
    pause issued inside another player callback is queued by ExoPlayer and the listener's stack would not show it.
  Hooks: one line each in `PlaybackFragment.createPlayer`, `PlaybackFragment.setPlayWhenReady`,
  `LeanbackActivity.dispatchKeyEvent` and `AppDialogActivity.dispatchKeyEvent`; lifecycle comes from the callbacks `FocusSpeed` already registers.
- **The panel-close guard** (`FocusPanels`): if the video was playing when a panel closed and is paused within 3 s,
  Focus resumes it once and logs who paused it. It leaves the pause alone when: the user pressed any key other than
  Back after the close; no pause event happened after the close (a replaced or released player); the engine is
  blocked (background, PIP) or the player is not the resumed screen (Home was pressed), re-checked each tick until the
  window ends because both can be brief during the switch back from the panel; the caller shows a media key,
  the media session or audio focus (`MediaSession`, `PlayPause`, `AudioFocus`), a phone remote (`RemoteController`),
  the sleep timer, or a frame-rate switch (`AutoFrameRate`/`AfrRestart`, which resumes on its own); the video has
  ended; or it was already paused at the close. Each decision is logged with its reason. *Claude: a guard rather than removing
  the pause at its source, because the source is not yet known; the new caller log will name it, and the source fix
  (and an upstream PR) can follow.*

## Consequences
- Logcat gets more lines per playback: a handful per play/pause and per key press. None fire per frame.
- If the guard's exclusions miss a deliberate pause (for example a pause from somewhere new within 3 s of closing a
  panel), Focus would resume it once; the log line says so, with the caller.
- Upstream verdict stays Pending until the caller is known and the same pause is seen on upstream SmartTube.

## Verified
(to fill in from the batch 6 build on the stick)
