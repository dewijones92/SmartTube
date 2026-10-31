# 0007. Fire Stick: stock app removed, Focus pinned first

- Status: Accepted
- Date: 2026-10-31
- Decided by: Dewi

## Decision
Upstream SmartTube beta was uninstalled from the Fire TV Stick (its settings backup is kept off-device).
SmartTube Focus was moved to the front of "Your apps and channels".

## Consequences
Restoring a beta settings backup into Focus brings the account across but its token does not work
("Access token is null!"); sign in fresh instead.
