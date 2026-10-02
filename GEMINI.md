# WANDR Project Rules (Gemini & Antigravity)

Refer to [`AGENTS.md`](file:///Users/martinkade/wandr/AGENTS.md) for full project rules and instructions.

## Key Rules Summary for Gemini / Antigravity:
1. **Clean Architecture & MVI**: Keep shared business logic in Kotlin Multiplatform (KMP). Use Koin for DI, Room for local persistence, Ktor for network.
2. **One UI Component Per File**: Each SwiftUI and Jetpack Compose component must have its own dedicated file.
3. **UI Previews**: All UI components must include preview annotations for Dark Mode and multiple screen sizes.
4. **Room Entities**: 1 entity class per file.
5. **Offline-First & Auto Token Refresh**: Room DB writes first, sync to Supabase with automatic 401 token renewal in Ktor.
6. **Privacy First**: Group challenges are team vs. team; other teams only see aggregated team totals, individual member progress stays within the own team.
7. **Permission Disclosure**: Show a localized dialog explaining *why* before any system permission prompt (location, camera, notifications, health data); see the `wandr-ui-components` skill.
8. **Skills**: Modular workflows are available under [`.agents/skills/`](file:///Users/martinkade/wandr/.agents/skills/).
