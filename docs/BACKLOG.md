# SmartTube Focus backlog

Work Dewi has asked for but not yet started. Top of the list goes first. No code for an item until Dewi says go.
When an item ships, remove it here and record the decision in an ADR.

## 1. Video stutters when the info or comments panel closes

**Asked:** Dewi, 2026-11-13. **Status:** not started, no code until Dewi says go.

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
