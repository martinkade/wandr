---
name: wandr-health-activities
description: >-
  Provides guidelines and workflows for integrating Apple Health, Health Connect,
  FIT file parsing/encoding (Garmin SDK), background GPS tracking, and WatchOS/WearOS companion apps.
---

# WANDR Health Integrations & Activity Tracking Skill

Use this skill when developing Apple Health / Health Connect data readers, background GPS tracking, FIT file export/import, or WatchOS/WearOS companion tracking.

> **Permissions:** Location, notification, HealthKit and Health Connect requests must follow the *Permission Disclosure* rule in the `wandr-ui-components` skill (disclosure dialog explaining why, then the system prompt, then an "Open Settings" fallback when denied).

---

## 🏃 Activity Tracking & FIT Format

1. **FIT Format Logging**:
   - Utilize Garmin FIT SDK (or Kotlin FIT wrapper) to decode and encode standard `.FIT` activity files.
   - Store GPS trackpoints, timestamps, altitude/ascent, heart rate, and distance.
   - **Keep FIT files on the device that recorded them** (`FitFileStorage`). Never upload them to Supabase and never include their path in sync payloads; only the activity metrics are synced.
2. **Background GPS Tracking**:
   - **Android**: Use a Foreground Service with a persistent system notification showing real-time distance and time.
   - **iOS**: Enable `location` background mode (`CLLocationManager` with `allowsBackgroundLocationUpdates = true`). Request "While Using" authorization only.
   - **Permission**: Show the location disclosure before the first system prompt (Android also asks for notifications on API 33+ in the same step).
   - Include map visualization support where available.

---

## 🏥 Health Platform Integrations (Read-Only)

1. **Apple HealthKit (iOS)**:
   - Request read-only permissions for `HKQuantityTypeIdentifierDistanceWalkingRunning`, `HKQuantityTypeIdentifierFlightsClimbed`, `HKWorkout`.
   - Map HealthKit workouts into WANDR `Activity` domain models.
2. **Health Connect (Android)**:
   - Request read permissions for `DistanceRecord`, `ElevationGainedRecord`, `ExerciseSessionRecord`.
   - Handle permission revocation and availability checks across API levels.

3. **Import pipeline (all external workouts)**:
   - Watch, Health Connect and HealthKit workouts all become a `WatchWorkout` (field `source`) and go through `WatchWorkoutInbox` → `WatchImportViewModel` → `ImportWatchWorkoutUseCase`. Never save them directly: the use case de-duplicates by id (use `ExternalWorkoutId.from(source, recordId)` for platforms without UUIDs) and routes overlaps to the conflict wizard.
   - Skip workouts written by WANDR itself (own package / bundle id prefix) to avoid duplicates.

---

## ⌚ Companion Apps (WatchOS & WearOS)

1. **WatchOS Companion**:
   - Standalone tracking session using `HKWorkoutSession`.
   - Sync live heart rate and distance metrics to main iOS app via `WatchConnectivity`.
   - Implemented in `iosApp/WandrWatch/` (XcodeGen target `WandrWatch`, bundle id `com.mediabeam.fitness.watchkitapp`, does **not** link the Kotlin `shared` framework). Logic lives in pure types (`WorkoutMetrics`, `WorkoutOutbox`, `WatchWorkout`); `WorkoutManager` wraps `HKWorkoutSession`/`HKLiveWorkoutBuilder`/`CLLocationManager`.
   - Wire format: `WatchWorkout` JSON (snake_case, see `shared/.../domain/watch/WatchWorkout.kt`), sent with `WCSession.transferUserInfo(["workout": json])`; the outbox file is re-sent on launch and cleared in `session(_:didFinish:error:)`. Keep the Swift `CodingKeys` in sync with the Kotlin model.
   - Show the permission disclosure before HealthKit/location requests (`PermissionDisclosureView`); strings live in `WandrWatch/Resources/{en,de}.lproj`.
2. **WearOS Companion**:
   - Jetpack Wear Compose UI.
   - Use Health Services API to record workouts and stream live stats to Android phone app.
   - Module `:wearApp` (`com.wandr.wear`, applicationId equals the phone app's so the Data Layer pairs them). Recording: `ExerciseTracker` (Health Services `ExerciseClient`) + `WorkoutForegroundService`; pure, unit-tested logic in `WorkoutAccumulator` and `WorkoutOutbox`/`OutboxSyncer`.
   - Transfer: `DataClient.putDataItem` on `/workouts/{id}` (urgent, key `json` = `WatchWorkoutCodec.encode`); the phone deletes the item as acknowledgement (`WorkoutAckService` removes it from the outbox); unacknowledged entries are re-sent on app start.
   - Only the wire format is shared via `:shared` (`com.wandr.domain.watch`). Wear UI: one composable per file, `@WearPreviews` (small/large round, font scale 1.5), strings in `values` + `values-de`.

---

## 🛠 Verification Checklist
- [ ] Are Apple Health and Health Connect permissions strictly read-only?
- [ ] Is background GPS tracking wrapped in a foreground service (Android) / background location updates (iOS)?
- [ ] Are activity logs exported in valid FIT format and kept on the recording device only (never uploaded)?
- [ ] Do WatchOS and WearOS apps handle connection state changes gracefully?
- [ ] Is a disclosure dialog shown before the location / Health permission prompts (see `wandr-ui-components`)?

## Android Push (FCM)
- Code in `androidApp/.../push/`: `WandrMessagingService` (token + foreground messages), `PushNotifier` (channel `social`, token fetch, local notification), `PushPayload` (pure payload mapping, unit-tested), `PushPermissionPrompt` (POST_NOTIFICATIONS with disclosure, asked once).
- Enable: download `google-services.json` from the Firebase console (package `com.mediabeam.fitness`) into `androidApp/`. It is git-ignored; the google-services plugin is only applied when the file exists. Without it all Firebase access is skipped (no crash, no pushes).
- Taps (own or FCM-displayed notifications) deliver `entity_type`/`entity_id` extras to `MainActivity`, which publishes a `PushTarget` via `PushNavigation`.
