# 0012. Tennis section, Kids mode, Moomoo bedtime, more Moomoo rows, Continue watching

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi (all five features; Alcaraz + Tennis TV latest; Kids exit = reopen the app; bedtime choosable,
  default 45 min; Bluey, Hey Duggee, CBeebies, Cocomelon; Continue watching as its own section; test-only build
  for bedtime); Claude (implementation, reset rule, icons)

## Decision
- **Tennis** (`FocusSections`, id 9002, after Moomoo, both modes): "Tennis TV · Latest" (channel
  `UCbcxFkd6B9xUU54InHv4Tig`, Latest chip) and "Carlos Alcaraz" (Tennis TV channel search "Alcaraz").
- **Continue watching** (id 9003, after Tennis, sign-in only): History filtered to items 5–95% watched, one row.
- **Kids** (third prompt button): `FocusMode.sKids` overlay on top of focus mode: sidebar = Moomoo only, opens on
  Moomoo, search/account top buttons hidden (`MainUIData.isTopButtonEnabled` hook). Ends on the next launch (the
  prompt resets it). The remote's search key is not blocked.
- **Bedtime** (`Moomoo.onTickle`, minute ticks): counts Moomoo playing time; at the limit shows "Last one! Bedtime
  after this episode 🌙", stops preloading, and the episode ends with PAUSE instead of rolling on. Resets after an
  hour without Moomoo. Limit in Settings › SmartTube Focus (`FocusSettings`, prefs `focus_mode/bedtime_minutes`):
  Off / 30 / 45 / 60 / 90, default 45.
- **More Moomoo rows**: Bluey `UCVzLLZkDuFGAE2BGdBuBNBg`, Hey Duggee `UC574PCE8QOIoXsOGAyL3obQ`, CBeebies
  `UC0fXNmjDoC7ckPyT6qM8Urw`, Cocomelon `UCbCmjCuTUZos6Inko4u57UQ`. Row loading moved to `FocusRows`.

## Consequences
- Bedtime granularity is one minute (tick-based); the warning comes during the episode that crosses the limit.
- Tested end to end with a test-only build (a Tennis TV row inside Moomoo and a 1-minute option), per the
  "tennis, never kids' videos" rule; that commit is dropped before merge.
