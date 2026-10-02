---
name: wandr-architecture
description: >-
  Provides guidelines and workflows for implementing Clean Architecture, MVI pattern,
  Koin dependency injection, Room entities, LRU file caching, and UseCases in the WANDR KMP project.
---

# WANDR Architecture & KMP Skill

Use this skill when creating or refactoring domain models, UseCases, repositories, cache modules, Koin modules, or ViewModels in the WANDR project.

---

## 🏛 Layering Rules & Guidelines

### 1. Domain Layer (`shared/domain`)
- **Entities**: Pure Kotlin data classes representing core domain objects (e.g., `User`, `Team`, `Challenge`, `Activity`).
- **UseCases**: Single-purpose execution blocks (e.g., `GetActiveGroupChallengesUseCase`, `JoinTeamViaInviteUseCase`).
  - Implements `invoke()` operator function.
  - Returns `Result<T>` or `Flow<T>`.
- **Repository Interfaces**: Define contracts for data access without any data source implementations.

### 2. Data Layer (`shared/data`)
- **Local Persistence (Room)**:
  - **Rule**: Each Room `@Entity` MUST be defined in its own individual `.kt` file under `data/local/entity/`.
  - Room DAOs return `Flow<T>` for reactive UI updates.
- **LRU File Cache (`data/cache`)**:
  - `LruFileCache` provides thread-safe file caching with adjustable byte limits (e.g., default 50 MB) and operation journaling (`READ`, `WRITE`, `REMOVE`, `EVICT`).
  - Use `LruFileCache` for caching cover photos (challenge/team covers) and user avatars.
  - **`.FIT` files do NOT belong in the cache and are never uploaded.** They are kept persistently on the recording device only, via `FitFileStorage` (app-private folder, deleted together with their activity). Sync payloads (`ActivityDto`) must not contain the file path.
- **Remote API (Ktor)**:
  - Intercepts requests using Supabase Auth JWT tokens.
  - Ktor HTTP client plugin handles automatic token refresh on `401 Unauthorized`.
- **Repository Implementations**:
  - Implements domain repository interfaces.
  - Mediates between Room local storage, LRU File Cache, and Supabase remote endpoints.

### 3. Presentation Layer (MVI Architecture)
- **State**: Single immutable data class representing the UI state.
- **Intent**: Sealed class/interface representing user actions (e.g., `TeamIntent.RefreshChallenges`).
- **Effect**: Single-shot events (e.g., navigation, toast notifications).
- **ViewModel**: Exposes `StateFlow<UiState>` and `SharedFlow<UiEffect>`, processes `Intent` via UseCases.

### 4. Dependency Injection (Koin)
- `sharedModule`: Combines `domainModule`, `dataModule`, `viewModelModule`.
- `platformModule`: Platform-specific implementations (e.g., Android Context, iOS HealthKit wrapper).

---

## 🛠 Verification Checklist
- [ ] Is business logic strictly located inside the `shared` KMP module?
- [ ] Are Room entities placed in their own separate files?
- [ ] Is media/workout caching routed through `LruFileCache`?
- [ ] Does the ViewModel communicate exclusively via UseCases?
- [ ] Are all dependencies registered in Koin modules?
