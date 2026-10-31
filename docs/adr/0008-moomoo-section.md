# 0008. Moomoo kids' section

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi (a section called Moomoo with a baby icon, directly below Channels, with popular Ms Rachel
  and Peppa Pig videos; two rows; visible in both modes; a video started there plays on through its row);
  Claude (implementation, icon drawing, end-of-row behaviour), accepted in the plan

## Decision
`common/.../misc/Moomoo.java` registers a row section (id 9001, "Moomoo", `icon_moomoo.png` drawn by
`focus/branding/make_moomoo_icon.py`) from one line in `BrowsePresenter.initSectionMappings`. Each row is
the channel's **Popular** sort chip (found by title, falling back to the second chip), loaded with
`continueGroup` and renamed "Ms Rachel · Popular" / "Peppa Pig · Popular":
- Ms Rachel - Toddler Learning Videos: `UCG2CL6EUjG8TVT1Tpl9nJdg`
- Peppa Pig - Official Channel: `UCAOtE1V7Ots4DjM8JLlrYgg`

`FocusMode.filterSections` inserts the section after Channels in both modes (only in the Focus build: the
section registers only when `focus_mode_available` is true).

Playback: `Video.isSectionPlaylistEnabled` is forced on for Moomoo videos, and
`VideoLoaderController.getPlaybackMode` returns "play continuously" while the row has another video, then
"pause" on the last one — *Claude, not separately asked*: without that, the last video would fall through
to YouTube's own up-next, which the decision rules out.

## Consequences
- Rows show the first page only (about 30 videos); the renaming wrapper reports no next page, because a
  non-internal group would take the legacy continuation path.
- If YouTube drops the Popular chip for a channel, that row falls back to the second chip (logged).
