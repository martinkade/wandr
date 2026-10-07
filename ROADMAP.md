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
  - **Android (Compose)**: `LoginScreen.kt`, `RegisterScreen.kt`, `ProfileScreen.kt`, `AvatarImagePicker.kt`. Include `@Preview` for Dark/Light & text scales.
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
- [x] **Step 4.4: Team memberships, priority & joining by invite (profile)**
  - The profile lists the user's groups (`TeamMembershipList`), reorderable by drag and drop (`team_members.priority`, saved via `set_team_priorities`), with a trailing item to join a group by typing the invite code or scanning its QR code (Google code scanner, no camera permission). Joining goes through `join_team_by_invite` (a non-member cannot read a team, so the previous client-side lookup could not work). The highest-priority group is the user's primary group (also used as the team for new activities).

---

### 🏆 Phase 5: Challenges & Team-vs-Team Standings (Privacy-First)

- [x] **Step 5.1: Challenge Domain Models & Lifecycle**
  - Define `Challenge` (Types: Distance, Elevation, Time; Scope: Group, Individual; Option: `requireAllMembersCompletion` for All-or-Nothing team completion).
  - **Group means team vs. team**: a group challenge is *not* tied to one team and its members do **not** compete against each other. Several teams enroll and compete as teams against the other enrolled teams; every member contributes to their own team's result. A user in several teams contributes for **one** team only: the one with the highest priority (the user orders their teams by drag and drop in the profile); the other teams' group challenges do not count their progress. An individual challenge is open to everyone and has no team.
  - Only `draft` (unpublished, visible to the creator only) and `active` (published) are stored (`challenge_status`, a toggle in the form; new challenges start as drafts). Start and end are picked as date + time, so challenges can be **planned in the future**. **Planned, Completed and Expired are derived at runtime** from the flag, the dates and the participants' progress (`EvaluateChallengeStatusUseCase`; Completed requires every member of a team when `requireAllMembersCompletion` is set), so they can never be stale.
- [x] **Step 5.2: Challenge Management & Role-Based Access**
  - Challenge creation is restricted to users with system `manager` role; editing, deleting and changing the cover is restricted to the **creator (owner)** of the challenge (buttons are only shown to them on the detail screen).
  - Support individual challenge join/quit for all users (joining only while the challenge is published and not over; a joined challenge shows "Leave" instead of "Join"), and group challenge **team enrollment/withdrawal by team owners & admins** (`challenge_teams`; enrolling adds all members as participants for their team).
  - Add cover photo upload to Supabase Storage (`challenge-covers` bucket).
- [x] **Step 5.1 addition: Counting activity types & server-side progress**
  - A challenge can define which activity types count (`activity_types`; none selected = every type counts), chosen in the challenge form (Android) and shown on the details. The participants' progress is calculated by the server from their activities (distance, elevation gain or duration of the activities that started within the challenge period and match the types; recalculated when activities or the challenge change, retroactively for new participants). Clients can no longer write progress. Activities count once synced.
  - Earlier activities count: a new participant (individual join, every member of an enrolled team,
    a team change) starts with the matching activities recorded before within the challenge period
    (`on_participant_init`); enrolling a team recalculates all participants. After joining,
    enrolling, leaving or withdrawing, the ranking and standings are reloaded. SUPABASE.md has a
    migration block for existing projects (includes a one-time backfill).
  - Activity routes (feed card, activity details; Android) are drawn on an OpenStreetMap background
    (`OsmTrackMap`): a static map without gestures. `StaticMapLayout` (shared, tested) fits the
    route and lists the 256 px tiles; Coil loads and caches them (`tile.openstreetmap.org`,
    identifying User-Agent, attribution shown as the OSM license requires). Without network the
    route shows on a plain background. The live recording screen uses the same map (position and
    route so far). iOS: `TrackMapView` uses the same shared layout, loads the tiles with
    `URLSession` (identifying User-Agent, `URLCache`) and shows the route in the feed card, the
    activity details and the recording (replaces MapKit). Tile caching: own 100 MB disk cache per
    platform (`OsmTiles` / Coil on Android, `OsmTileCache` / URLCache on iOS); tiles stay valid 30
    days (Android) or until evicted (iOS) instead of the server's week, and the tiles of a route are
    prefetched when a recording is saved, so the map also shows offline. The tile URL lives in
    `StaticMapLayout.TILE_URL_TEMPLATE`. An interactive map is not planned yet. Note: the public OSM
    tile server is for light use only; switch to a tile provider before a wide release.
  - Android: the challenge and group details use `CollapsingHeaderScaffold` (top bar + cover header
    that scrolls away with a parallax effect; the bar turns solid and shows the title).
    `ChallengeHero` became the shared `CoverHero`; the challenge menu and the group's "Edit" are top
    bar actions; the group page got a challenge-like sheet with the avatar on the cover's edge.
  - Android: the activity details show the route map as the collapsing header
    (`CollapsingHeaderScaffold`, parallax); the route stays clear of the top bar and the sheet
    (`StaticMapLayout.fit` got separate top/bottom padding). Without a route the header is the
    contour cover. The prefetch of map tiles on save covers this header size too.
  - Android: the recording notification is a Live Update (Android 16+): `POST_PROMOTED_NOTIFICATIONS`, `ProgressStyle` with the progress towards the next full kilometer, `setRequestPromotedOngoing` and the recording time ("Pause" while paused) as the status bar chip. Older versions show the ongoing notification with a plain progress bar. The user can switch Live Updates off per app in the system settings.
  - iOS: the running recording is a Live Activity (lock screen, Dynamic Island; iOS 17+): new widget extension `WandrLiveActivity` (`project.yml`, shared `Shared/RecordingActivityAttributes.swift`) with the running time (counts up by itself), the distance and a pause / resume button (LiveActivityIntent, runs in the app without opening it). `RecordingLiveActivityController` starts, updates (distance at most every 5 s) and ends it from `LiveGpsTrackingView`, which now also keeps the recording time and distance (they were never updated before). `LiveGpsTrackingView` is now reachable: a record button in the feed's toolbar opens it as a full-screen cover. It uses the shared recording view model (`RecordingObserver`; GPS fixes and the clock feed it, the same as on Android), shows map, distance, time and pace, pause / resume and a finish dialog (save, discard, continue) with the conflict wizard. The recording runs in the app-wide `RecordingCoordinator` (GPS fixes, clock, Live Activity), so its screen can be closed while it runs; the feed's record button turns red and opens it again. Before the start the screen shows the GPS status and the current position on the map.
- [x] **Step 5.3: Team-vs-Team Standings (Privacy-First)**
  - Rank **teams** against each other by the sum of their members' progress (`challenge_team_standings`, aggregates only). Other teams never see individual members; the members of a team see each other's progress within their own team.
  - Compute percentage progress towards target values (e.g. 100km, 5000m, 5h).
- [x] **Step 5.4 addition: Ranking in the challenge details**
  - The details show who is ahead: team standings for group challenges, plus the member ranking (`challenge_member_ranking`): everybody in an individual challenge (for participants and the creator), only the own team in a group challenge. Your own row is highlighted; equal progress shares a rank.
- [x] **Step 5.4 addition: Challenge details & list redesign (Android)**
    - The details open with a hero (cover photo, or a gradient with elevation lines that differ per
      challenge), the back button on top and a sheet sliding over its lower edge; the hexagon emblem
      of the challenge (symbol of distance / elevation / time) sits on the edge. Below: centered
      title and description, facts with icons (length and "ends in 3 days", goal, counting
      activities, scope), the join / leave / enroll action, then team standings, ranking and
      comments. Editing moved into a "..." menu of the creator; changing the cover photo works by
      tapping the hero.
    - The list item uses the same structure (cover, emblem, title, description, key facts, status
      pill) and no longer has a join / leave button: participation is decided on the details.
- [x] **Step 5.4 addition: Hero transition & team-admin-only enrolling (Android)**
  - The badge of the tapped challenge flies from the list item to its place on the details page
    while the page slides in, and flies back when it is left, by button or predictive back gesture
    (`HeroTransitionState`, `heroSource` / `heroTarget`, `HeroLayer`, driven by the progress of
    `SlideInOverlay`). Opening the details without a tap (notification) just slides.
  - Group challenges can only be enrolled or withdrawn by owners / admins of a team
    (`availableChallengeAction` takes the administered and the already enrolled team ids): default
    members see no button, the choice dialog only offers administered teams that are not enrolled
    yet, and the ViewModel rejects other teams (the server enforces it as well).
- [x] **Step 5.4: Challenge & Leaderboard UI**
  - Build `ChallengeListScreen`/`View`, `ChallengeCard.kt`/`.swift` (group challenges: "Enroll Team" instead of "Join"), `LeaderboardRow.kt`/`.swift` (members of the own team).

---

### 🏃 Phase 6: Activity Logging, GPS Tracking & Manual History

- [x] **Step 6.1: Activity Domain & Data Layer**
  - Create `Activity` domain entity & `ActivityEntity` in Room.
- [x] **Step 6.2: Manual Activity Entry & Editing**
  - Build UI and UseCases for creating/editing manual logs (e.g. "Hiked 10km yesterday").
  - Android: FAB menu in the feed (create manually / record). Manual entry and editing use a bottom sheet; only the **owner** can edit, from the details screen (measured values of recorded activities stay read-only). The details screen shows the track if the FIT file is on this device.
  - The owner can delete an activity from its details (with confirmation). Deleting marks the row, the sync engine deletes it on the server (a plain local delete would let the next pull bring it back) and a trigger removes its likes, comments and notifications; the challenge progress is recalculated. Every local activity change (create, edit, delete) now starts a sync right away; before, changes were only uploaded at the next app start, and a change that arrives during a running sync triggers one more run.
- [x] **Step 6.3: Garmin FIT SDK Integration**
  - Implement `.FIT` file encoder/decoder to save/parse trackpoints, distance, duration, elevation.
  - Store `.FIT` files **on the recording device only** (app-private folder via `FitFileStorage`); they are never uploaded to Supabase. Only the activity metrics are synced; the local file path is not part of the sync payload.
- [x] **Step 6.4 addition: Recording UI (Android)**
  - Strava-like recording screen (`LiveGpsTrackingScreen`): the route over a backdrop (no map tiles), a GPS status box (searching / weak / good, from the fix accuracy) with the clock, pace (speed for cycling) and distance, which expands to a full-screen data overlay (big clock, pace, distance and the times of the last kilometers) and collapses back; sport selector, start / pause / resume and finish (save, discard or continue).
  - The recording runs in a process-wide view model (`RecordingScope`) fed by `LocationTrackingService`, a real foreground service with fused location and a live notification (distance, time). Sampling: one data point per second for fast sports (cycling, running), one every 3 seconds for hiking (`RecordingPolicy`). The clock counts moving time (paused time is excluded), the distance skips the way covered while paused, the elevation gain is measured with a jitter threshold. On saving, the track becomes the FIT file on the device and the encoded polyline for the server (step 6.3 addition).
- [x] **Step 6.4: Live GPS Tracking & Background Service**
  - **Android**: Foreground service with ongoing system notification displaying live distance/time.
  - **iOS**: Background location updates using `CLLocationManager`.
- [x] **Step 6.3 addition: Route polyline & map privacy**
  - The recorded track is simplified (Douglas-Peucker, max. 500 points) and encoded with Google's Encoded Polyline Algorithm (`PolylineCodec`); only this route is stored on the server, in its own table `activity_routes` (the FIT file still never leaves the device). The owner decides per activity whether others may see the map (`Activity.showMap`, switch in the edit sheet); because row-level security cannot hide a single column, the route table's policy only returns routes with `show_map = true` to team members, the owner always sees their route.
- [x] **Step 6.5: Track Map Visualization**
  - Render GPS track polyline on dynamic map view.

---

### ⚔️ Phase 7: Activity Time Conflict Resolution Wizard

- [x] **Step 7.1: Overlap Detection Algorithm**
  - `ActivityRepository.getOverlappingActivities` (Room query) implements:
    $$\text{Overlap} \iff (\text{Start}_A < \text{End}_B) \land (\text{End}_A > \text{Start}_B)$$
    Activities that merely touch do not conflict. `ActivityConflictResolver` applies it to the user's own activities (the edited activity is excluded) in the create, update and GPS-recording use cases. Editing without changing the time range skips the check, so overlaps that already exist never block e.g. a rename.
- [x] **Step 7.2: Conflict Resolution Wizard UI**
  - Nothing is saved while a conflict is open; the use case fails with `ActivityConflictException` and `ActivityViewModel` exposes `ActivityState.conflict`. Android: `ActivityConflictSheet` (bottom sheet, choose + apply) in the history, details and recording screens. The user selects:
    - **Merge**: Combine metrics into a single workout (distance/elevation summed, time = whole range, available tracks joined); the overlapping activities are replaced.
    - **Trim**: Automatically adjust boundaries: the new activity is cut to the longest free part of its time range, metrics scaled proportionally, track filtered. Not offered if it is completely covered.
    - **Discard**: Keep original activity and delete conflicting record (the new/edited one, or the finished recording).
  - A finished recording cannot be dismissed without a choice, so it is never lost silently. iOS UI is still open.

---

### ⌚ Phase 8: WearOS & WatchOS Companion Tracking Apps

- [x] **Step 8.1: WatchOS Companion App**
  - SwiftUI watch app `WandrWatch` (`iosApp/WandrWatch/`, XcodeGen target embedded in `Wandr`, watchOS 11): type picker, live view (time, distance, ascent, heart rate), pause/resume/stop, summary.
  - `HKWorkoutSession` + `HKLiveWorkoutBuilder` + GPS route (`HKWorkoutRouteBuilder`), `workout-processing` background mode; permission disclosure precedes the HealthKit/location prompts.
  - Finished workouts go to the iPhone as JSON (`WatchWorkout`, same wire format as shared) via `WCSession.transferUserInfo`, with a persistent outbox re-sent on launch.
- [x] **Step 8.2: WearOS Companion App**
  - Module `:wearApp` (Compose for Wear OS Material 3, standalone, same applicationId as the phone app): type picker, start, live workout (pause/resume/stop) and summary screens, permission disclosure before every system prompt.
  - Health Services `ExerciseClient` (heart rate, distance, ascent, GPS) inside a foreground service; `WorkoutAccumulator` builds the `WatchWorkout`.
  - Finished workouts go to a file outbox and via Data Layer `/workouts/{id}` (`json` = `WatchWorkoutCodec`); the phone's deletion of the item acknowledges it, unsent entries are re-sent on app start. Unit tests in `wearApp/src/test`.

---

### 🏥 Phase 9: Apple Health & Health Connect Integrations (Read-Only)

- [x] **Step 9.1: Apple HealthKit Integration (iOS)**
  - Read-only: the app requests read access for workouts, walking/running and cycling distance (never write access) after a disclosure alert, and reads workouts since the last import (first: 30 days) via the "Import" button of the Apple Health card in the profile (`HealthKitImporter`). Elevation comes from the workout's ascent metadata. Only hiking/walking, running and cycling are imported; workouts written by WANDR itself (the watch app) are skipped.
- [x] **Step 9.2: Health Connect Integration (Android)**
  - The import is started from a card in the profile (`HealthImportCard`). Requests only `READ_EXERCISE`, `READ_DISTANCE` and `READ_ELEVATION_GAINED` (after the disclosure dialog; the rationale activity required by Health Connect is included). Exercise sessions are read since the last import, distance/elevation are aggregated per session from the session's own data origin (`HealthConnectImporter`). Revoked permission or missing Health Connect are handled with dialogs/messages.
- [x] **Step 9.1/9.2 addition: Steps & Floors**
  - Both platforms also read today's steps and floors climbed (Health Connect `StepsRecord`/`FloorsClimbedRecord`, HealthKit `stepCount`/`flightsClimbed`, aggregated by the platform so data from several apps is not double counted) and show them on the health card in the profile. They are read-only, shown on the device and not stored or synced; they are requested together with the workout permissions after the disclosure (steps/floors are optional, workouts work without them). Challenges still use activities only.
- [x] **Step 9.3: Import De-duplication**
  - Imports use the same pipeline as watch workouts (`WatchWorkoutInbox` → `ImportWatchWorkoutUseCase`): the activity id is derived from the platform's record id (`ExternalWorkoutId`), so reading a workout twice imports it once, and an overlap with existing activities opens the Conflict Resolution Wizard (merge / trim / discard).

---

### 💬 Phase 10: Social Interactions & Notifications

- [x] **Step 10.1: Group Feed, Comments & Likes**
  - `Comment`/`Like` models and `SocialRepository` (Supabase only: social data needs connectivity, no local cache), `SocialViewModel` per activity/challenge with optimistic likes. Plain-text comments (max. 1000 characters) can be created and edited by their author and deleted by the author **or the owner of the activity/challenge** (`SocialState.canEdit/canDelete`, enforced by RLS as well). Comments, likes and reactions are only visible to those who can see the activity (owner/team members) or the published challenge.
  - Group feed: the feed (`FeedScreen`) has a "Mine | Team" switch; `ActivityFeedRepository` pulls the team members' activities (and the user's own from other devices) into the local cache. Android: `SocialSection` in the activity and challenge details; iOS: `SocialSectionView` in `ActivityDetailView`.
- [x] **Step 10.1 addition: Fast feed cards**
  - The feed loads like and comment counts of ALL its cards in one call (`social_counts`, at most 100 ids per call, indexed) instead of two queries per card, and the authors' profiles in one query; `ActivityCard` shows author, date, distance/pace/time/elevation, the route (a light canvas drawing, no map tiles per card) and likes/comments with an optimistic like button.
- [x] **Step 10.2: Comment Reactions**
  - Emoji reactions (`Reactions.allowed`) toggled per comment, shown as counters with the own reaction highlighted (`comment_reactions`).
- [x] **Step 10.3: Push Notifications Engine**
  - Server: triggers write `notifications` for likes, comments and reactions (never for your own actions); the Edge Function `supabase/functions/send-push` delivers them via FCM (Android) and APNs (iOS) to the user's `device_tokens` and removes invalid tokens. See `SUPABASE.md` → "Push notifications" for deployment.
  - Clients: `PushTokenManager` (shared) registers the token via the `register_device_token` RPC after sign-in and removes it on logout. Android: `WandrMessagingService` (needs `androidApp/google-services.json`, see the skill), iOS: `PushAppDelegate` (needs the Push Notifications capability/provisioning). A disclosure dialog explains the permission before the system prompt; tapping a push opens the activity/challenge. In-app notification list with unread badge on both platforms.

---

## 🛡️ Security & Feature Requirements Verification (`FEATURES.md` vs `SUPABASE.md` & Project Code)

| Requirement (`FEATURES.md`) | `SUPABASE.md` DDL / RLS Policy | Project Implementation | Status |
| :--- | :--- | :--- | :---: |
| 1. Only `manager` role can create challenges; only the creator can edit/delete them (and change the cover) | `INSERT` on `challenges` checks `system_role = 'manager'`; `UPDATE`/`DELETE` and the `challenge-covers` storage policies check `created_by = auth.uid()` | `Profile.systemRole`; `ChallengeState.canEdit` (creator only) drives the detail screen buttons | ✅ Verified |
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

