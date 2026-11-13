# SmartTube Focus backlog

Work Dewi has asked for but not yet started. Top of the list goes first. No code for an item until Dewi says go.
When an item ships, remove it here and record the decision in an ADR.

## 1. Video stutters when the info or comments panel closes

**Asked:** Dewi, 2026-11-13. **Status:** in progress (batch 5, ADR 0014): instrumented with `FocusPanels`, cause
not yet proven.

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
