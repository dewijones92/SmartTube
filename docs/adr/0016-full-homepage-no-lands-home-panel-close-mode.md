# 0016. Batch 8: a "❌ Full homepage" section, "No" lands on Home (panel-close refresh switch still open)

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
  it is animating, which looked like the cause (disproved below).

## Decision
- **❌ Full homepage.** `FocusSections` registers section 9005, "❌ Full homepage" with a red X icon
  (`icon_full_home.png`, `focus/branding/make_section_icons.py`), whose rows come from the same
  `ContentService.getHomeObserve()` upstream uses for Home. A one-line hook in `BrowsePresenter.isHomeSection()` treats
  it as Home, so upstream's Home row filter (Primetime, News and similar rows removed) and its 3-hour auto-refresh
  apply too. It is added only in distraction-free mode, just above Settings; Kids mode never shows it, and "No" mode
  has the normal Home.
- **"No" lands on Home.** After "No", if the browse screen resumes within 5 s, `FocusMode` waits 800 ms (the sidebar's
  selection callback lands late) and selects Home again if something else is showing, logging what it found. A later
  resume, or switching back to focus mode, cancels it, so it never overrides a section the user picked.
- **Panel-close refresh-rate switch: not fixed in this batch.** First hypothesis (Android ignores an animating window's
  preferred mode) was tested and disproved on the stick, 32.76-focus.50, 11:49: with the close animation removed
  (`overridePendingTransition(0, 0)`, logged as applied) the TV still switched to config 33 (59.94 Hz) 100 ms after the
  close and back to config 28 (50 Hz) 0.46 s later. Polling `dumpsys window` through the close showed the player window
  asking for `preferredDisplayMode=5` the whole time and the panel window too until it went, so no window attribute
  asks for 59.94. The change was removed. Next suspects: the video surface's own frame-rate vote when the player
  resumes, or Amazon's HDMI handling. Carried to the backlog.

## Consequences
- The sidebar label is "Full homepage" with the red X as its icon (an emoji in the label doubled the X and was cut off).

## Verified
On the stick, 32.76-focus.50, 2026-11-16, TV on:
- 11:31: "No, full SmartTube" landed on Home (Recommended rows); log "No: browse screen back on Home as chosen".
- 11:47: distraction-free sidebar shows the red-X "Full homepage" just above Settings; opening it shows the full
  YouTube Home (Recommended, Start a music mix). Kids mode doesn't get it (filterSections returns early for Kids).
- No `FATAL EXCEPTION` across the runs.
