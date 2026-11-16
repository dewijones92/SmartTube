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
