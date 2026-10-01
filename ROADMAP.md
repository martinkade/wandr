# WANDR — Implementation Plan & Feature Roadmap

This document serves as the master implementation plan and roadmap for **WANDR**. It breaks down the specifications into bite-sized, incremental milestones and features.

---

## 🎯 Architecture & Technology Stack Overview

- **Core**: Kotlin Multiplatform (KMP) Shared Module (`shared/`)
- **Architecture**: Clean Architecture + MVI (Model-View-Intent) + UseCases
- **Dependency Injection**: Koin
- **Local Persistence**: Room Multiplatform (1 `@Entity` per file)
- **Networking & Auth**: Ktor + Supabase Kotlin SDK (Auto 401 Token Refresh)
- **iOS UI**: SwiftUI (Native Apple Liquid Glass styling, 1 component per file, mandatory previews)
- **Android UI**: Jetpack Compose (Native Material 3 Expressive styling, 1 component per file, mandatory previews)
- **Watch Companion**: WatchOS (SwiftUI + HealthKit) & WearOS (Compose for WearOS + Health Services)
- **Health Integrations**: Read-only Apple Health & Health Connect
- **Activity Recording**: Garmin FIT SDK, background GPS tracking with persistent notification

---

## 🗓 Implementation Roadmap

```
┌─────────────────────────────────────────────────────────────────────────┐
│ Phase 1: Project Foundation & KMP Setup                                 │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 2: Auth, Session & User Profiles                                  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 3: Offline-First Engine & Storage (Room + Ktor Sync)              │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 4: Teams & Group Management                                       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 5: Challenges & Team-Scoped Leaderboards                          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 6: Activity Recording & Manual History                            │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 7: Conflict Resolution Wizard                                     │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 8: WearOS & WatchOS Companion Apps                                │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 9: Apple Health & Health Connect Integrations                     │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│ Phase 10: Social Interactions & Push Notifications                      │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 🧱 Phase 1: Project Foundation & KMP Architecture Setup

- [x] **Step 1.1: Multiplatform Gradle Configuration**
  - Setup root `build.gradle.kts`, `shared/`, `androidApp/`, `iosApp/` modules.
  - Configure Kotlin 2.x, Compose Multiplatform plugin, and target SDK settings.
- [x] **Step 1.2: Dependency Injection (Koin)**
  - Configure `sharedModule`, `domainModule`, `dataModule`, `viewModelModule`.
  - Implement platform-specific Koin initialization for Android (`Context`) and iOS.
- [x] **Step 1.3: Room Multiplatform Database Setup**
  - Configure Room Gradle plugin & KSP for KMP.
  - Define `WandrDatabase` class and initial migration setup.
- [x] **Step 1.4: Ktor Network Client & Supabase SDK Initialization**
  - Setup Ktor HTTP Client with ContentNegotiation (kotlinx.serialization) & Logging.
  - Configure automatic `401 Unauthorized` token refresh interceptor.

---

### 🔐 Phase 2: Authentication, Sessions & User Profiles

- [x] **Step 2.1: Supabase Auth Core**
  - Implement `AuthRepository` (Login, Register, Logout, Password Reset, Email/Password).
  - Implement session storage & automatic token renewal logic in Ktor.
- [x] **Step 2.2: User Profile Domain & Data Layer**
  - Create `Profile` domain entity, `ProfileEntity` (Room, 1 file), and `ProfileRepository`.
  - Implement avatar upload to Supabase Storage (`avatars` bucket).
- [x] **Step 2.3: Auth & Profile UI Components**
  - **Android (Compose)**: `LoginScreen.kt`, `RegisterScreen.kt`, `ProfileScreen.kt`, `AvatarPicker.kt`. Include `@Preview` for Dark/Light & text scales.
  - **iOS (SwiftUI)**: `LoginView.swift`, `RegisterView.swift`, `ProfileView.swift`, `AvatarPickerView.swift`. Include `#Preview` for Light/Dark color schemes.

---

### 💾 Phase 3: Offline-First Sync Engine & Storage

- [x] **Step 3.1: Room Local-First Write Layer**
  - Add `sync_status` (`PENDING`, `SYNCED`, `DIRTY`) to Room entities.
  - Implement DAOs with reactive `Flow` emissions.
- [x] **Step 3.2: Bidirectional Sync Worker Engine**
  - Create `SyncManager` background engine to detect local `DIRTY` records and push to Supabase Postgres.
  - Pull remote updates from Supabase and reconcile local Room state.
- [x] **Step 3.3: Sync Engine Testing**
  - Write KMP unit tests in `commonTest` for offline enqueueing, online flushing, and failure retries.

---

### 👥 Phase 4: Teams & Group Management

- [x] **Step 4.1: Team Domain & Data Layer**
  - Define `Team` (with avatar & `cover_url`) and `TeamMember` entities, `TeamRepository`.
  - Implement team creation, update, deletion, and cover photo upload to Supabase Storage (`team-covers` bucket).
- [x] **Step 4.2: Invite Link, QR Code & Deep Linking System**
  - Implement 8-character unique `invite_code` generation and validation.
  - Handle QR Code generation and deep link resolution (`wandr://invite/{code}`).
- [x] **Step 4.3: Team Management UI**
  - Build `CreateTeamScreen`/`View`, `TeamDetailsScreen`/`View` (with Team Cover banner), `MemberListScreen`/`View`, `TeamInviteQRCodeScreen`/`View`.
  - Ensure 1 UI component per file with Dark Mode previews and localization (EN & DE).

---

### 🏆 Phase 5: Challenges & Team-Scoped Leaderboards (Privacy-First)

- [ ] **Step 5.1: Challenge Domain Models & Lifecycle**
  - Define `Challenge` (Types: Distance, Elevation, Time; Scope: Group, Individual; Option: `requireAllMembersCompletion` for All-or-Nothing team completion).
  - Implement status evaluation (Planned, Active, Completed [requiring 100% member completion when enabled], Expired/Failed).
- [ ] **Step 5.2: Challenge Management & Admin Rights**
  - Restrict challenge creation and user participation to group admins.
  - Add cover photo upload to Supabase Storage (`challenge-covers` bucket).
- [ ] **Step 5.3: Privacy-First Leaderboards**
  - Calculate leaderboard rankings strictly within team scope (`WHERE team_id = :teamId`).
  - Compute percentage progress towards target values (e.g. 100km, 5000m, 5h).
- [ ] **Step 5.4: Challenge & Leaderboard UI**
  - Build `ChallengeListScreen`/`View`, `ChallengeCard.kt`/`.swift`, `LeaderboardRow.kt`/`.swift`.

---

### 🏃 Phase 6: Activity Logging, GPS Tracking & Manual History

- [ ] **Step 6.1: Activity Domain & Data Layer**
  - Create `Activity` domain entity & `ActivityEntity` in Room.
- [ ] **Step 6.2: Manual Activity Entry & Editing**
  - Build UI and UseCases for creating/editing manual logs (e.g. "Hiked 10km yesterday").
- [ ] **Step 6.3: Garmin FIT SDK Integration**
  - Implement `.FIT` file encoder/decoder to save/parse trackpoints, distance, duration, elevation.
  - Store `.FIT` files in Supabase Storage (`fit-files` bucket).
- [ ] **Step 6.4: Live GPS Tracking & Background Service**
  - **Android**: Foreground service with ongoing system notification displaying live distance/time.
  - **iOS**: Background location updates using `CLLocationManager`.
- [ ] **Step 6.5: Track Map Visualization**
  - Render GPS track polyline on dynamic map view.

---

### ⚔️ Phase 7: Activity Time Conflict Resolution Wizard

- [ ] **Step 7.1: Overlap Detection Algorithm**
  - Implement overlap detection logic in `ActivityRepository`:
    $$\text{Overlap} \iff (\text{Start}_A < \text{End}_B) \land (\text{End}_A > \text{Start}_B)$$
- [ ] **Step 7.2: Conflict Resolution Wizard UI**
  - Build `ConflictWizardDialog`/`Sheet` allowing the user to select:
    - **Merge**: Combine metrics into a single workout.
    - **Trim**: Automatically adjust boundaries.
    - **Discard**: Keep original activity and delete conflicting record.

---

### ⌚ Phase 8: WearOS & WatchOS Companion Tracking Apps

- [ ] **Step 8.1: WatchOS Companion App**
  - Build WatchOS app using SwiftUI.
  - Implement `HKWorkoutSession` for live tracking.
  - Sync metrics with primary iOS app via `WatchConnectivity`.
- [ ] **Step 8.2: WearOS Companion App**
  - Build WearOS app using Compose for WearOS.
  - Integrate Health Services API for real-time tracking.
  - Sync metrics with primary Android app.

---

### 🏥 Phase 9: Apple Health & Health Connect Integrations (Read-Only)

- [ ] **Step 9.1: Apple HealthKit Integration (iOS)**
  - Request read permissions for Distance, Workouts, and Elevation.
  - Auto-import workouts into WANDR local database.
- [ ] **Step 9.2: Health Connect Integration (Android)**
  - Request read permissions for `DistanceRecord`, `ElevationGainedRecord`, `ExerciseSessionRecord`.
- [ ] **Step 9.3: Import De-duplication**
  - Ensure HealthKit/Health Connect imports trigger the Conflict Resolution Wizard if overlaps exist.

---

### 💬 Phase 10: Social Interactions & Notifications

- [ ] **Step 10.1: Group Feed, Comments & Likes**
  - Implement `Comment` and `Like` data models and repositories.
  - Plaintext comment creation, editing, and deletion by activity owner.
- [ ] **Step 10.2: Comment Reactions**
  - Emoji reactions on comments (`comment_reactions`).
- [ ] **Step 10.3: Push Notifications Engine**
  - Configure Firebase Cloud Messaging (FCM) & Apple Push Notification service (APNs).
  - Trigger instant notifications for new likes, comments, and reactions.
