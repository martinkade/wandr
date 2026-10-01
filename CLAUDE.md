# CLAUDE.md - WANDR Architecture & Coding Guidelines

This guide helps Claude understand the project structure, commands, and rules for WANDR.

## Project Overview
WANDR is a cross-platform fitness app (iOS & Android) built with Kotlin Multiplatform (KMP), Clean Architecture, MVI, Supabase Auth/Postgres, Apple Health, Health Connect, and WatchOS/WearOS companion apps.

## Primary Rules & Conventions
- **Clean Architecture & MVI**: Shared business logic in KMP (`shared/`). Use UseCases, Repositories, ViewModels, and MVI state management.
- **Dependency Injection**: Use Koin (`shared/di/`).
- **Data Persistence**: Room Multiplatform database (`shared/data/local/`). **Strict rule**: Place every `@Entity` in its own separate file.
- **Networking**: Ktor client (`shared/data/remote/`) with automatic `401 Unauthorized` token refresh handling Supabase Auth.
- **UI Components**:
  - **SwiftUI** (iOS): Native Liquid Glass aesthetic.
  - **Jetpack Compose** (Android): Native Material 3 Expressive aesthetic.
  - **Strict rule**: 1 file per UI component.
  - **Strict rule**: Every component MUST provide previews for Dark Mode and multiple display sizes.
  - **Strict rule**: Show a disclosure dialog explaining *why* before any system permission prompt (location, camera, notifications, health data) — see `wandr-ui-components` skill, "Permission Disclosure".
- **Testing**: Write Unit tests for UseCases, Repositories, and Sync logic in `commonTest`.

## Skills Directory
For detailed step-by-step procedures, refer to skills in [`.agents/skills/`](file:///Users/martinkade/wandr/.agents/skills/):
- `wandr-architecture`: Guidelines on Clean Architecture, MVI, and KMP modules.
- `wandr-ui-components`: UI guidelines for SwiftUI & Jetpack Compose.
- `wandr-sync-offline`: Offline-first Room DB, Supabase sync engine, and token refresh.
- `wandr-health-activities`: Apple Health, Health Connect, WearOS/WatchOS companion & FIT logging.
