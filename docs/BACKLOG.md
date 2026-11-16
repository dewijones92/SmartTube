# SmartTube Focus backlog

Work Dewi has asked for but not yet started. Top of the list goes first. No code for an item until Dewi says go.
When an item ships, remove it here and record the decision in an ADR.

## 1. Voice typing into any text box (hold the remote's mic button)

**Asked:** Dewi, 2026-11-16. **Status:** discussing, not started.

**Wanted:** open any text box (SmartTube's search box, ideally any app on the Fire Stick), hold the remote's voice
button, speak, and have the words typed in. Keep the Alexa "search YouTube for ..." bridge (ADR 0014). Dewi thinks
holding the button over a text box worked before.

**Starting points (unverified):** the stick has no speech recogniser for apps (no `RecognitionService`, ADR 0014), so
SmartTube's own mic can't work; Fire OS's own keyboard may offer dictation when it is on screen. Check which keyboard
SmartTube's search box opens and whether Fire OS dictation fills it.

## 2. Short hitch when closing the info or comments panel (TV refresh rate switches twice)

**Asked:** Dewi, 2026-11-16. **Status:** investigated, not fixed (ADR 0016).

**What happens:** with AFR on, closing the panel makes the TV switch 50 -> 59.94 -> 50 Hz within half a second; HDMI
audio drops ~1.5 s and the picture stands still ~1 s. Playback itself keeps going (ADR 0015 fixed the real pause).

**Known:** not a window's preferred mode (player and panel windows both ask for mode 5 throughout); removing the close
animation did not help; turning ALL system animations off (2026-11-16 13:09) did not help either. `dumpsys display`
through the close shows DisplayModeDirector's app-requested mode flip from 5 (50 Hz) to 3 (59.94) within ~40 ms of
the panel closing and back ~0.4 s later: for a moment no window's request counts, and the 60 Hz default wins.
Disproved 2026-11-16 13:39 (32.77-focus.53, branch `batch9`, not shipped): bringing the player's task to the front
(moveTaskToFront, no animation) BEFORE the panel finishes; the switch still happened (config 33 then 28). Amazon's docs
promise no switch when both screens prefer the same mode, which looks broken here. Remaining routes: show panels inside
the player window (big upstream change), report it to Amazon, or turn AFR off. Parked by default. **Next:** capture `dumpsys display` (DisplayModeDirector votes) and SurfaceFlinger frame-rate
votes during the close; check whether the video surface's `setFrameRate` vote is cleared and re-set when the player
resumes; test whether it happens with the stick's own "match frame rate" instead of SmartTube's AFR.
