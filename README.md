# WANDR — Multiplatform Fitness & Challenge App

**WANDR** is a modern, cross-platform fitness app for iOS and Android designed for outdoor enthusiasts, hikers, and team challenges. Built on **Kotlin Multiplatform (KMP)** with an offline-first architecture, WANDR seamlessly connects outdoor activity recording with privacy-focused group challenges and social interaction.

---

## 🚀 Key Features

### 🔐 Auth & Data Sync (Offline-First & Caching)
- **Supabase Integration**: Auth, Postgres Database, and Storage (avatars, team covers, challenge cover images).
- **Session Management**: Automatic token refresh on `401 Unauthorized`.
- **Offline-First Sync**: Local state managed with Room; bidirectional sync engine pushes and pulls updates when connectivity is restored.
- **LRU File Cache & Journaling**: Thread-safe multiplatform file cache with adjustable byte limits (e.g. 50 MB default) and operation journaling (`READ`, `WRITE`, `REMOVE`, `EVICT`) for cover photos, avatars, and workout files.

### 🏆 Teams & Challenges
- **Group Management**: Create, join via invite link, and administer teams.
- **Challenge Types**:
  - **Distance**: e.g., 100 km in 30 days.
  - **Elevation Gain**: e.g., 5,000 m ascent in 30 days.
  - **Active Time**: e.g., 5 hours of activity in 30 days.
- **Lifecycle & Status**: Draft and Active are stored; Completed and Expired are derived at runtime from the start/end date and progress. Custom cover photo, descriptions, and targets.
- **Group Challenges**: Teams compete **against other teams** (not members against each other). Team owners/admins enroll their team, all members contribute to the team's result, and the standings rank teams.
- **Privacy**: Other teams only see aggregated team totals; individual progress is visible within your own team only (no global cross-group tracking).

### ⌚ Health & Activity Recording
- **Health Integrations**: Read-only integration with **Apple Health** (iOS) and **Health Connect** (Android).
- **Companion Apps**: Dedicated **WearOS** and **WatchOS** apps for real-time activity tracking.
- **GPS & FIT Logging**: Live tracking (including background recording with persistent system notification) exported as standard `.FIT` files (Garmin SDK) that are stored on the recording device only, never uploaded.
- **Manual Entry & History**: Erfassen & Editieren von Aktivitäten (z. B. „Gestern 10 km gewandert“).
- **Time Conflict Wizard**: Automated detection of overlapping activities with an interactive resolution wizard.

### 💬 Social Interactions
- Group activity feed with plaintext comments, reactions, and likes.
- Granular permissions: Activity owners can edit or delete comments on their posts.
- Instant notifications for likes, comments, and reactions.

---

## 🛠 Tech Stack & Architecture

WANDR follows **Clean Architecture** combined with the **MVI (Model-View-Intent)** pattern.

```
                  ┌─────────────────────────────────────────┐
                  │              UI Layer                   │
                  │   iOS (SwiftUI)    Android (Compose)    │
                  └──────────────────┬──────────────────────┘
                                     │ Intent / State
                  ┌──────────────────▼──────────────────────┐
                  │             ViewModel                   │
                  └──────────────────┬──────────────────────┘
                                     │ UseCases
                  ┌──────────────────▼──────────────────────┐
                  │           Domain Layer                  │
                  │     (Entities & UseCase Interfaces)     │
                  └──────────────────┬──────────────────────┘
                                     │ Repositories
                  ┌──────────────────▼──────────────────────┐
                  │            Data Layer                   │
                  │   Room (Local DB)   Ktor (Supabase DB)  │
                  └─────────────────────────────────────────┘
```

| Layer / Aspect | Technology | Details |
| :--- | :--- | :--- |
| **Shared Business Logic** | **Kotlin Multiplatform (KMP)** | Clean Architecture, MVI, UseCases, Repositories |
| **Dependency Injection** | **Koin** | KMP-native DI for shared & platform modules |
| **Local Storage** | **Room** | Multiplatform SQLite persistence (1 entity per file) |
| **Media & File Cache** | **LRU File Cache** | Thread-safe multiplatform LRU file cache with adjustable byte limits & operation journaling |
| **Networking** | **Ktor** | HTTP Client with automatic 401 token refresh plugin |
| **iOS UI** | **SwiftUI** | Native Apple Liquid Glass aesthetic + previews |
| **Android UI** | **Jetpack Compose** | Native Material 3 Expressive UI + previews |
| **Watch Apps** | **WatchOS / WearOS** | Companion tracking apps |

---

## 📏 Coding & Development Conventions

To maintain code quality and smooth developer / AI agent collaboration:

1. **Granular Component Architecture**:
   - Every UI component resides in its **own file**.
   - Create reusable shared components rather than monolithic views.
2. **UI Previews**:
   - Provide previews for **every** UI component with annotations for both **Dark Mode** and **multiple screen sizes** (for both SwiftUI and Jetpack Compose).
3. **Clean Code & Self-Documenting Logic**:
   - Keep KDoc and inline comments concise; write descriptive naming over verbose comments.
4. **Room Entities**:
   - Place each Room `@Entity` in its **own separate file**.
5. **Testing**:
   - Mandatory unit tests for UseCases, Repositories, and Sync logic.

---

## 🤖 AI Agent & Skill System (`.agents`, `GEMINI.md`, `CLAUDE.md`)

This repository is optimized for AI-assisted development (Antigravity / Gemini, Claude Code, Cursor, Windsurf).

### Agent Configuration Files
- **[`AGENTS.md`](file:///Users/martinkade/wandr/AGENTS.md)** / **[`GEMINI.md`](file:///Users/martinkade/wandr/GEMINI.md)**: Universal project rules, architecture guidelines, and coding standards.
- **[`CLAUDE.md`](file:///Users/martinkade/wandr/CLAUDE.md)**: Targeted guidelines for Claude agent workflows.
- **[`.agents/skills/`](file:///Users/martinkade/wandr/.agents/skills/)**: Modular runbooks for specific dev workflows:
  - `wandr-architecture`: Clean Architecture, MVI, KMP module structure & Koin DI rules.
  - `wandr-ui-components`: SwiftUI & Jetpack Compose guidelines, previews & Liquid Glass / M3 design.
  - `wandr-sync-offline`: Room DB, Supabase sync engine, 401 auto-renew & conflict wizard.
  - `wandr-health-activities`: Apple Health, Health Connect, WearOS/WatchOS companion & FIT logging.

---

## 📄 Roadmap

For full business requirements and detailed specs, see **[`ROADMAP.md`](file:///Users/martinkade/wandr/ROADMAP.md)**.
