---
name: wandr-sync-offline
description: >-
  Provides guidelines and workflows for implementing offline-first storage with Room DB,
  Supabase Auth token auto-refresh (401 handling), sync engine, and the activity conflict resolution wizard.
---

# WANDR Offline-First & Data Sync Skill

Use this skill when implementing local storage, sync mechanisms, Supabase network requests, token refresh logic, or activity conflict resolution.

---

## 💾 Offline-First Pattern

1. **Local Writes First**:
   - UI intents trigger updates directly to local Room SQLite DB.
   - Local records are flagged with `sync_status = PENDING` or `DIRTY`.
2. **Background Sync Engine**:
   - Periodic or network-triggered sync worker pushes dirty local records to Supabase Postgres.
   - On successful HTTP 200/201 response, local `sync_status` is updated to `SYNCED`.

---

## 🔒 Supabase Auth & Automatic 401 Token Refresh

1. **Ktor Token Refresh Plugin**:
   - Configure Ktor `Auth` / `BearerTokens` plugin.
   - When a network call returns `401 Unauthorized`:
     - Intercept call automatically.
     - Call Supabase refresh token endpoint.
     - Update active session token in secure local storage.
     - Automatically retry original request with new token.

---

## ⚔️ Activity Conflict Resolution Wizard

1. **Detection**:
   - When saving a new activity or syncing from HealthKit/Health Connect, check for time overlaps against existing recorded activities:
     $$\text{Overlap} \iff (\text{Start}_A < \text{End}_B) \land (\text{End}_A > \text{Start}_B)$$
2. **Wizard Execution**:
   - If an overlap is detected, trigger the `ConflictResolutionWizard` UI flow.
   - Options presented to user:
     - **Merge**: Combine overlapping GPS/distance metrics.
     - **Trim**: Automatically adjust start/end boundaries of conflicting activities.
     - **Discard/Keep Original**: Choose primary activity and discard duplicate.

---

## 🔒 Privacy-First Leaderboards

- **Strict Enclosure**: Leaderboards are strictly calculated within team bounds (`WHERE team_id = :teamId`).
- **No Global Leakage**: Do not expose or aggregate user activities across different teams or global user bases.

---

## 🛠 Verification Checklist
- [ ] Are Room DB writes local-first before attempting remote sync?
- [ ] Is 401 token refresh handled transparently by Ktor client?
- [ ] Is conflict detection invoked upon activity creation/sync?
- [ ] Are team leaderboards strictly scoped to the team ID?
