# 0006. Own signing key; versionCode scheme

- Status: Accepted
- Date: 2026-10-31
- Decided by: Claude, accepted in the plan

## Decision
- A dedicated RSA-4096 key (alias `focus`), stored as repo secrets `SIGNING_KEY`, `KEY_STORE_PASSWORD`,
  `KEY_PASSWORD`. The alias is hard-coded, not a secret: a secret value masks that word in every log.
- `versionCode = upstream versionCode × 1000 + workflow run number` (passed as `-PfocusBuild`), and
  `versionName = <upstream>-focus.<run>`, so every build is newer to Android and to the updater.

## Consequences
Losing the key means future updates cannot install over the existing app. A copy must live outside the
laptop (the local copy is not in any automatic backup).
