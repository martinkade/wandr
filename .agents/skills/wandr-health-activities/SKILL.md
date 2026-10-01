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

---

## ⌚ Companion Apps (WatchOS & WearOS)

1. **WatchOS Companion**:
   - Standalone tracking session using `HKWorkoutSession`.
   - Sync live heart rate and distance metrics to main iOS app via `WatchConnectivity`.
2. **WearOS Companion**:
   - Jetpack Wear Compose UI.
   - Use Health Services API to record workouts and stream live stats to Android phone app.

---

## 🛠 Verification Checklist
- [ ] Are Apple Health and Health Connect permissions strictly read-only?
- [ ] Is background GPS tracking wrapped in a foreground service (Android) / background location updates (iOS)?
- [ ] Are activity logs exported in valid FIT format?
- [ ] Do WatchOS and WearOS apps handle connection state changes gracefully?
- [ ] Is a disclosure dialog shown before the location / Health permission prompts (see `wandr-ui-components`)?
