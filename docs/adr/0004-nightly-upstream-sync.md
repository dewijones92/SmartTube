# 0004. Nightly upstream merge, publish, in-app updater

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi (stay current with upstream automatically; updates reach the TV through the in-app
  updater only, not the Pi; conflicts reported as a GitHub issue plus email; public fork);
  Claude (workflow design), accepted in the plan

## Decision
`.github/workflows/nightly-sync.yml` runs at 03:00 UTC and on demand (`force_build` input):
fetch `yuliskov/SmartTube` master → stop if nothing new → merge → on conflict open or comment on one issue
titled "Nightly upstream merge conflict" and fail → build `assembleStfocusRelease` → sign → write
`smarttube_focus.json` → push the merge → publish a versioned release and replace the `latest` release.
The app's `update_urls` points at `releases/download/latest/smarttube_focus.json`.
Upstream's own workflows are disabled on the fork.

## Consequences
- Every `gh` call passes `--repo "$GITHUB_REPOSITORY"`: with a remote named `upstream`, gh otherwise
  targets yuliskov/SmartTube (this broke the first release and issue).
- Only `master` publishes; other branches can be used to test the conflict path.
- A clean merge can still break behaviour silently; hooks are one-liners to keep that rare.
