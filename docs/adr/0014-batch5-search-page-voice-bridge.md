# 0014. Batch 5: no flash after the prompt, a Search page, History in Kids, Alexa voice search

- Status: Accepted
- Date: 2026-11-13
- Decided by: Dewi (the chosen screen must appear without a glimpse of another one; Back from search lands on an
  empty page with the sidebar and Search; History in every mode, Kids included, listing everything watched; voice
  search through the remote via our own Alexa bridge, not the phone-keyboard QR route); Claude (how each is built,
  and the items marked below)
- Amends [ADR 0013](0013-hidden-prefetch-search-first.md): the focus-mode boot section is now the Search page,
  not Channels.

## Context
Dewi reported, on 32.72-focus.28: a glimpse of the other screen after answering the prompt, Subscriptions-like
content behind the search page, no History in Kids, and a mic that hands over to Alexa instead of searching
SmartTube. On the stick (2026-11-13, TV off): there is no speech recogniser for apps (no `RecognitionService`, no
`RECOGNIZE_SPEECH` activity), so SmartTube's own mic and "Instant voice search" cannot work; the assist key opens
Alexa (`com.amazon.vizzini`). Upstream's Fire TV answer is a bridge app installed as `com.amazon.firetv.youtube`,
which Fire OS opens for "Alexa, search YouTube for ..." when the YouTube app is absent. Its manifest and dex
(checked 2026-11-13) only launch `org.smarttube.stable`, `org.smarttube.beta` or `app.smarttube.fdroid`, so on this
stick it would open the hidden upstream test copy, not Focus.

## Decision
- **No flash after the prompt.** Yes, No and Kids now open their screen (search page, Home, Moomoo) first and close
  the black prompt only once it has drawn: for Yes, two frames after the search screen resumes (1.5 s fallback); for
  No and Kids, which change the section behind the prompt, after 300 ms. Opening first and closing straight away still
  showed about four frames of the browse screen on the emulator.
- **Search page.** In focus mode a "Search" section sits first in the sidebar and is the boot section. Its page is
  upstream's settings-style tile grid with one tile, Search, which opens the search page. Back from search lands
  here. *Claude: reusing the settings grid rather than a new view.* A settings password, if one is ever set, also
  asks before this tile, because the grid's click handler checks it.
- **History in Kids.** The Kids sidebar is Moomoo, then History, added even if History isn't pinned.
- **Leftover player off the Back stack.** A relaunch while a video was open left the player under the new screen, so
  Back from search reopened it (seen on the stick: a kids' video resumed at 50 min). Any prompt choice now removes that
  player from SmartTube's screen stack, so Back cannot reach it. It is not finished: finishing it makes SmartTube start
  its parent (Browse) in front of the prompt, which is the flash this batch removes. *Claude, not separately asked.*
- **No search in Kids, voice included.** Kids already hides the search buttons; a voice search arriving through the
  bridge while Kids is on is now blocked too (`SearchPresenter` hook). *Claude, not separately asked.*
- **Upstream's bridge installer hidden.** About › "Enable voice search" would install upstream's bridge over ours (it
  only opens upstream's packages), so it is hidden in Focus.
- **Voice bridge.** `focus/bridge` is a separate small app, package `com.amazon.firetv.youtube`, same intent filters
  as upstream's bridge. It forwards links unchanged and turns a bare search query into
  `youtube://search?query=...&isVoice=true` (the form SmartTube already parses) for `org.smarttube.focus`; a link
  with no query of its own gives way to the spoken query extra. It is a
  standalone Gradle project (no change to upstream's `settings.gradle`), signed with the Focus key, built by the
  nightly in a step that cannot block Focus, and published as `smarttube_focus_voice_bridge.apk`. It is installed
  once by hand; the in-app updater does not update it.
- **Cold search launch is distraction-free.** When the process starts from a search link (the bridge), focus mode
  is switched on as the app starts (the search screen opens just after, asynchronously); otherwise a cold link launch
  kept the stored full mode. Warm launches keep
  the current mode as before. *Claude, not separately asked.*
- **Panel pause (backlog item 1) is instrumented, not fixed.** `FocusPanels` logs position, play state and
  loading for 8 s after any side panel that shrinks the video opens or closes (info, comments, quality, speed and the
  other `fitVideoIntoDialog` callers), ignoring a video that has simply ended, and says how long playback stood still, via
  one-line hooks in `BasePlayerController`'s fit-video runnables. The first measurement on .28 was invalid (see
  Consequences), so the fix waits for evidence from this build.

## Consequences
- The empty Search page replaces Channels as what sits behind search; Channels is still in the sidebar.
- Kids can browse History, which includes grown-up videos (Dewi accepted).
- Testing notes for the panel issue: on this stick `screenrecord` and SurfaceFlinger `--latency` do not see the video
  (hardware overlay), but `screencap | md5sum` in a loop does; DPAD_RIGHT while the player controls are hidden seeks
  forward, so check the info button is focused (uiautomator) before pressing OK.
- Voice search depends on Fire OS routing "search YouTube" to `com.amazon.firetv.youtube`; proven only once Dewi
  speaks a search on the real remote.

## Verified (2026-11-13, build 32.74-focus.36 + bridge 1.0.36, Fire Stick with the TV off, emulator)
- Voice: Dewi held the remote's mic and said "search YouTube for tennis"; logcat showed the bridge forwarding
  `youtube://search?query=tennis&isVoice=true`, Focus starting cold in distraction-free mode, and the search results
  on screen. Alexa does not react hands-free to speech from the soundbar: this remote needs the mic button held.
- Yes on the stick: screen recording goes prompt, search screen background, search page, with no browse frames.
  (The emulator, which draws far slower, still showed 2-3 near-black browse frames, because upstream's
  `AppDialogActivity.onUserLeaveHint` finishes the prompt as soon as search starts.)
- Back from search lands on the Search page; No lands on Home; Kids shows Moomoo and History; Home on the prompt keeps
  focus mode. No `FATAL EXCEPTION` on the stick across the run.
- Not yet verified: the panel instrumentation (two attempts were spoilt by the test harness) and the leftover-player
  path on a real relaunch.
