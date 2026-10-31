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

## Amendment 2026-10-31 (review findings)
- The workflow checks out with a **write deploy key** (`DEPLOY_KEY` secret; public half registered as a repo
  deploy key, private copy in `~/.credentials/smarttube-focus-deploy-key`). `GITHUB_TOKEN` can never push a
  merge that changes `.github/workflows/*`, which upstream does regularly.
- Order is build → push → publish (a release cannot target a commit GitHub doesn't have yet). A failed push
  opens (or comments on) an issue titled "Nightly sync could not push the upstream merge" and stops before
  publishing, so a stuck sync is never silent. APKs are uploaded before the JSON so the updater never sees a
  new version number pointing at an old APK. `latest` is updated in place
  (`gh release upload --clobber`), so the updater URL never 404s mid-run.
