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
│ Phase 5: Challenges & Team-vs-Team Standings                            │
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
  - Create `Profile` domain entity (including `systemRole`: `'user'`, `'manager'`), `ProfileEntity` (Room, 1 file), and `ProfileRepository`.
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
  - Implement team creation (restricted to users with `manager` role), update (owner & admin), deletion (owner with `manager` role), and cover photo upload to Supabase Storage (`team-covers` bucket).
- [x] **Step 4.2: Invite Link, QR Code & Member Roles**
  - Implement 8-character unique `invite_code` generation, QR Code invite system (`wandr://invite/{code}`), and member leaving capabilities.
  - Enforce ownership & admin controls: Only team owners can assign admin role to members or remove members.
- [x] **Step 4.3: Team Management UI**
  - Build `CreateTeamScreen`/`View`, `TeamDetailsScreen`/`View` (with Team Cover banner), `MemberListScreen`/`View`, `TeamInviteQRCodeScreen`/`View`.
  - Ensure 1 UI component per file with Dark Mode previews and localization (EN & DE).

---

### 🏆 Phase 5: Challenges & Team-vs-Team Standings (Privacy-First)

- [x] **Step 5.1: Challenge Domain Models & Lifecycle**
  - Define `Challenge` (Types: Distance, Elevation, Time; Scope: Group, Individual; Option: `requireAllMembersCompletion` for All-or-Nothing team completion).
  - **Group means team vs. team**: a group challenge is *not* tied to one team and its members do **not** compete against each other. Several teams enroll and compete as teams against the other enrolled teams; every member contributes to their own team's result. An individual challenge is open to everyone and has no team.
  - Only `draft` (unpublished, visible to the creator only) and `active` (published) are stored (`challenge_status`, a toggle in the form; new challenges start as drafts). Start and end are picked as date + time, so challenges can be **planned in the future**. **Planned, Completed and Expired are derived at runtime** from the flag, the dates and the participants' progress (`EvaluateChallengeStatusUseCase`; Completed requires every member of a team when `requireAllMembersCompletion` is set), so they can never be stale.
- [x] **Step 5.2: Challenge Management & Role-Based Access**
  - Restrict challenge creation, editing, and deletion exclusively to users with system `manager` role.
  - Support individual challenge join/quit for all users, and group challenge **team enrollment/withdrawal by team owners & admins** (`challenge_teams`; enrolling adds all members as participants for their team).
  - Add cover photo upload to Supabase Storage (`challenge-covers` bucket).
- [x] **Step 5.3: Team-vs-Team Standings (Privacy-First)**
  - Rank **teams** against each other by the sum of their members' progress (`challenge_team_standings`, aggregates only). Other teams never see individual members; the members of a team see each other's progress within their own team.
  - Compute percentage progress towards target values (e.g. 100km, 5000m, 5h).
- [x] **Step 5.4: Challenge & Leaderboard UI**
  - Build `ChallengeListScreen`/`View`, `ChallengeCard.kt`/`.swift` (group challenges: "Enroll Team" instead of "Join"), `LeaderboardRow.kt`/`.swift` (members of the own team).

---

### 🏃 Phase 6: Activity Logging, GPS Tracking & Manual History

- [x] **Step 6.1: Activity Domain & Data Layer**
  - Create `Activity` domain entity & `ActivityEntity` in Room.
- [x] **Step 6.2: Manual Activity Entry & Editing**
  - Build UI and UseCases for creating/editing manual logs (e.g. "Hiked 10km yesterday").
- [x] **Step 6.3: Garmin FIT SDK Integration**
  - Implement `.FIT` file encoder/decoder to save/parse trackpoints, distance, duration, elevation.
  - Store `.FIT` files **on the recording device only** (app-private folder via `FitFileStorage`); they are never uploaded to Supabase. Only the activity metrics are synced; the local file path is not part of the sync payload.
- [x] **Step 6.4: Live GPS Tracking & Background Service**
  - **Android**: Foreground service with ongoing system notification displaying live distance/time.
  - **iOS**: Background location updates using `CLLocationManager`.
- [x] **Step 6.5: Track Map Visualization**
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

---

## 🛡️ Security & Feature Requirements Verification (`FEATURES.md` vs `SUPABASE.md` & Project Code)

| Requirement (`FEATURES.md`) | `SUPABASE.md` DDL / RLS Policy | Project Implementation | Status |
| :--- | :--- | :--- | :---: |
| 1. Only `manager` role can create/edit/delete challenges | `INSERT`, `UPDATE`, `DELETE` on `challenges` check `system_role = 'manager'` | `Profile.systemRole` in KMP domain model & Room entity | ✅ Verified |
| 2. Only `manager` role can create/delete groups/teams | `INSERT`, `DELETE` on `teams` check `system_role = 'manager'` | Supported in `TeamRepository` & KMP Profile | ✅ Verified |
| 3. Only team owners can assign `admin` role | `UPDATE` on `team_members` checks `created_by = auth.uid()` | `UpdateTeamMemberRoleUseCase` checks owner | ✅ Verified |
| 4. Only team owners can remove members | `DELETE` on `team_members` checks `created_by = auth.uid()` OR `user_id = auth.uid()` | `DeleteTeamMemberUseCase` | ✅ Verified |
| 5. Team owners & admins can edit team details | `UPDATE` on `teams` checks `created_by = auth.uid()` OR `role = 'admin'` | `UpdateTeamUseCase` | ✅ Verified |
| 6. All users can join/quit individual challenges | `INSERT`/`DELETE` on `challenge_participants` allows `user_id = auth.uid()` for `individual` | `JoinChallengeUseCase` / `QuitChallengeUseCase` | ✅ Verified |
| 7. Owners & admins can enroll/withdraw their team in group challenges (teams compete against each other) | `INSERT`/`DELETE` on `challenge_teams` allow owners/admins via `is_team_admin`; triggers add/remove the members as participants | `EnrollTeamInChallengeUseCase` | ✅ Verified |
| 8. Join groups via invite code / QR code | `invite_code` column + `INSERT` on `team_members` | `TeamInviteQRCodeScreen.kt` & `.swift` | ✅ Verified |
| 9. Group members can leave group/team | `DELETE` on `team_members` allows `user_id = auth.uid()` | `LeaveTeamUseCase` | ✅ Verified |
| 10. All users can update their profile | `UPDATE` on `profiles` allows `auth.uid() = id` | `UpdateProfileUseCase` | ✅ Verified |
| 11. All users can create/update/delete activities | `INSERT`/`UPDATE`/`DELETE` on `activities` allows `auth.uid() = user_id` | Phase 6 Activity UseCases & Repositories | ✅ Verified |

