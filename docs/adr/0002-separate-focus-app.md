# 0002. Ship as a separate app, `org.smarttube.focus`

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi (fork rather than settings only, after trying a settings-only set-up on 2026-10-29);
  Claude (package name, variant mechanics), accepted in the plan

## Context
SmartTube's own settings could hide feeds, but could not ask at launch or offer a per-session choice.

## Decision
A new product flavour `stfocus` in `smarttubetv/build.gradle`: applicationId `org.smarttube.focus`,
label "SmartTube Focus". Library modules (including the two submodules) have no `stfocus` flavour, so the
app flavour uses `matchingFallbacks = ['stbeta']`. Everything Focus-specific (icons, colours, update URL,
the `focus_mode_available` flag, `searchable.xml` authority) lives in `smarttubetv/src/stfocus`, where it
overrides library resources. `org.smarttube.focus` is added to `Utils.KNOWN_PACKAGES` so the full About
screen (update settings) and backup restore treat it as a SmartTube build.

## Consequences
- Installs beside upstream SmartTube without provider clashes; needs its own sign-in.
- Submodules stay unforked.
- Firebase/Crashlytics is not applied (it is gated to Stbeta/Ststable tasks).
