# 0001. Record decisions as ADRs

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi

## Decision
Keep a short record of each decision in `docs/adr/`, one file per decision, so the reasoning survives
alongside the code. New decisions get the next number; a reversed decision is marked Superseded and links
to the record that replaces it.

## Consequences
These files live only in the fork, so upstream merges never touch them.
