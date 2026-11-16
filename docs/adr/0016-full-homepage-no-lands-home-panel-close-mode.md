# 0016. Batch 8: a "❌ Full homepage" section, "No" lands on Home, no refresh-rate switch when a panel closes

- Status: Accepted
- Date: 2026-11-16
- Decided by: Dewi (a full homepage reachable from distraction-free mode as a sidebar section, like Tennis, never the
  default, marked with a red X; "No, full SmartTube" opens on Home; fix the few-second hitch after closing description
  or comments); Claude (how each is built; placement just above Settings, distraction-free only, recommended and taken
  as the default while Dewi answered the other question)

## Context
- Distraction-free mode hides Home, Shorts and the other feeds (ADR 0003) and opens on the plain Search page
  (ADR 0014). Dewi wants the full YouTube homepage one deliberate step away, not gone.
- 2026-11-16 11:02 on the stick (32.75-focus.46): choosing "No, full SmartTube" logged "landing: Home" but the screen
  showed Moomoo. Cause not yet proven; the selection is by list index while the sidebar is rebuilt.
- Closing the info or comments panel (11:05, same build): `hwcomposer` switched HDMI to 59.94 Hz (config 18) at the
  moment the player resumed, then back to 50 Hz (config 16) 0.46 s later; HDMI audio dropped about 1.5 s and the
  picture stood still about 1 s. With the panel open, `dumpsys window` showed both SmartTube windows asking for mode 5
  (50 Hz), so nothing of ours reset the mode. Android 11's refresh-rate policy ignores a window's preferred mode while
  it is animating; closing the panel animates a task switch, so for the length of the animation the only window left
  is the launcher, at the default 59.94 Hz.

## Decision
- **❌ Full homepage.** `FocusSections` registers section 9005, "❌ Full homepage" with a red X icon
  (`icon_full_home.png`, `focus/branding/make_section_icons.py`), whose rows come from the same
  `ContentService.getHomeObserve()` upstream uses for Home, so the content is exactly SmartTube's Home. It is added
  only in distraction-free mode, just above Settings; Kids mode never shows it, and "No" mode has the normal Home.
- **"No" lands on Home.** After "No", when the browse screen resumes (two frames after, like the prompt close in
  ADR 0014), `FocusMode` checks the selected section and selects Home again if it isn't Home. Each step is logged with
  the section it found, so the cause shows in logcat.
- **No animation when a panel closes while a frame-rate mode is held.** A one-line hook after `finishReally()` in
  `AppDialogActivity.finish()` calls `FocusPanels.skipCloseAnimation`, which calls `overridePendingTransition(0, 0)`
  when the sticky AFR mode is active (ADR 0011). Without the animation the player's 50 Hz request is never ignored.
  Logged either way.

## Consequences
- Panels and other SmartTube dialogs close instantly, with no fade, while an AFR mode is held.
- Upstream: the panel-close switch is upstream behaviour (any AFR user with a dialog in its own task); a PR candidate
  once proven on the TV (rule 2: exact build, evidence comment).

## Verified
(to fill in from the batch 8 build on the stick)
