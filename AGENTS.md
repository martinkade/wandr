# WANDR Project Rules & Agent Guidelines

This document defines the strict rules, architectural guidelines, and code conventions for AI agents (Gemini, Claude, Antigravity, Cursor, Windsurf, etc.) working on the **WANDR** codebase.

---

## 🏗 Architecture Requirements

WANDR uses **Kotlin Multiplatform (KMP)** with **Clean Architecture** and **MVI (Model-View-Intent)**.

### 1. Layered Architecture & Dependency Flow
- **Domain Layer (`shared/domain`)**:
  - Contains Models/Entities, UseCase interfaces/classes, and Repository interfaces.
  - Zero framework dependencies (Pure Kotlin).
- **Data Layer (`shared/data`)**:
  - Implements Repository interfaces.
  - Handles Room local database persistence and Ktor network calls to Supabase.
  - Every Room entity MUST be in its own separate file.
- **Presentation Layer (`shared/presentation` & platform UI)**:
  - Exposes `ViewModel` and MVI `State`, `Intent`, `Effect`.
  - Native UI views observe State and emit Intents.
  - **Android UI**: Jetpack Compose using Material 3 Expressive.
  - **iOS UI**: SwiftUI using Apple Liquid Glass styling.

### 2. Dependency Injection (Koin)
- Use Koin for KMP dependency injection.
- Define explicit modules for `domainModule`, `dataModule`, `viewModelModule`, and platform-specific modules (`platformModule`).

---

## 🎨 UI Component Conventions

1. **One Component Per File**:
   - Every UI component MUST have its own individual file. Avoid multi-component files.
2. **Previews Required**:
   - **Android (Jetpack Compose)**: Include `@Preview` for Light Mode, Dark Mode (`UI_MODE_NIGHT_YES`), and multiple font/screen scale configurations.
   - **iOS (SwiftUI)**: Include `#Preview` for Light/Dark color schemes and different device sizes.
3. **Design Aesthetic**:
   - Android: Modern Material 3 Expressive, dynamic colors, glassmorphism cards.
   - iOS: Native SwiftUI with Liquid Glass blur effects, smooth spring animations.
4. **Permission Disclosure**:
   - Before any system permission prompt (location, camera, notifications, health data) that is not granted yet, show a localized disclosure dialog explaining *why* the permission is needed. Only then request it; offer "Open Settings" when it was permanently denied. See the `wandr-ui-components` skill, "Permission Disclosure".

---

## 🔄 Offline-First & Sync Rules

1. **Local-First Writes**:
   - User actions write immediately to the local Room SQLite DB.
2. **Sync Engine**:
   - Background worker/sync manager listens for local dirty state and syncs with Supabase Postgres.
3. **Automatic 401 Token Refresh**:
   - Ktor HTTP client must intercept `401 Unauthorized` responses and automatically renew the Supabase Auth session token before retrying requests.
4. **Conflict Resolution**:
   - Overlapping activities trigger the Conflict Resolution Wizard (time overlap detection).

---

## 🧪 Quality & Testing Standards

1. **Unit Tests**:
   - Write KMP unit tests for all UseCases, Repositories, and Sync engine logic in `commonTest`.
2. **Minimal Inline Documentation**:
   - Rely on clean, self-documenting code with expressive naming. Limit KDoc/comments to complex edge cases or mathematical algorithms.
3. **Privacy First**:
   - Group challenges are **team vs. team**: members of one team do not compete against each other. Standings rank teams using aggregated totals only (`challenge_team_standings`); the progress of individual members is visible within their own team only. Never write code that exposes members of other teams.

---

## 🛠 Available Skills (`.agents/skills/`)

When working on specific sub-tasks, refer to or activate the following skills:
- [`wandr-architecture`](file:///Users/martinkade/wandr/.agents/skills/wandr-architecture/SKILL.md)
- [`wandr-ui-components`](file:///Users/martinkade/wandr/.agents/skills/wandr-ui-components/SKILL.md)
- [`wandr-sync-offline`](file:///Users/martinkade/wandr/.agents/skills/wandr-sync-offline/SKILL.md)
- [`wandr-health-activities`](file:///Users/martinkade/wandr/.agents/skills/wandr-health-activities/SKILL.md)
