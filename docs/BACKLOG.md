# SmartTube Focus backlog

Work Dewi has asked for but not yet started. Top of the list goes first. No code for an item until Dewi says go.
When an item ships, remove it here and record the decision in an ADR.

## 1. Video stutters when the info or comments panel closes

**Asked:** Dewi, 2026-11-13. **Status:** in progress: reproduced 2026-11-13 22:58 (a real pause); batch 6 (ADR 0015)
adds a resume guard and logs the pause's caller. Remove this item once the batch 6 build is verified on the TV.

**What happens:** while a video is playing, pressing the info icon opens the info panel at the side. Closing it
makes the video stop, then resume after a gap of a few seconds. Comments do the same: they open at the side, and
closing them restarts playback oddly.

**Wanted:** the video keeps playing, without a pause or gap, while the panel opens and after it closes.
"If this is not too hard to fix."

**Starting points (unverified, read before planning):**
- Both panels are `AppDialogActivity` (a separate translucent activity) over the player. The likely cause is the
  player activity's pause and resume around that dialog, or the surface being re-attached when it closes.
- Check first whether it is upstream behaviour or ours: reproduce on upstream SmartTube with the same video. Batch 4
  added an `onResume` hook to `AppDialogActivity` (`FocusMode.applyPromptBackground`), which only acts while the
  launch prompt is up, so it should not be the cause, but rule it out.
- If it is upstream, it is a strong upstream PR candidate (rule 2).
- Test on the emulator or the stick with the TV off, with a tennis video, and prove it from logcat (rule 10).
- 2026-11-13 findings: the info panel calls `fitVideoIntoDialog` (shrinks the video beside the panel and restores it on
  close); logcat showed "Could not find corresponding native window for surface" on close. Not proven as the cause:
  the first measurement was spoilt by DPAD_RIGHT seeking. Next: read `FocusPanels` lines from the batch 5 build.
- Batch 5 ships `FocusPanels`: next time it happens on the TV, note the time; `adb logcat | grep 'dewidebug panel'`
  shows position and play state every 500 ms for 8 s after the panel opens and closes.
- 2026-11-13 22:58 REPRODUCED on the stick (TV on, .37, tennis cCQS2fUyDGk): info panel open 6 s, playback fine
  (position +500 ms per tick, playWhenReady=true). Within 500 ms of the panel closing (Back), playWhenReady=false,
  loading=false, engineBlocked=false, position frozen for the whole 8 s window, and still paused at 3:02 a minute later.
  So it is a PAUSE call, not a stall or rebuffer. mFitVideoFinish only resets zoom/gravity. Suspect the Back that closes
  the dialog also reaching the player. Next: log who calls setPlayWhenReady(false)/pause around the close.
