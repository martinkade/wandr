---
name: wandr-ui-components
description: >-
  Provides standards and workflows for building UI components in SwiftUI (iOS)
  and Jetpack Compose (Android), including preview annotations, dark mode support,
  internationalization/localization (English & German), modular file layout, and the permission
  disclosure flow (explain why before any system permission prompt: location, camera, notifications,
  photos, health data).
---

# WANDR UI Components Skill

Use this skill whenever creating, editing, or styling UI components for Android (Jetpack Compose) or iOS (SwiftUI), **and whenever a feature needs a runtime/system permission** (location, camera, notifications, photos, microphone, Bluetooth, HealthKit / Health Connect).

---

## 🎨 General Principles

1. **One Component Per File**:
   - Never combine multiple standalone UI components into a single file.
   - Example: `ChallengeCard.kt` contains ONLY `ChallengeCard`, `ParticipantAvatarPill.swift` contains ONLY `ParticipantAvatarPill`.

2. **Design Language**:
   - **Android**: Material 3 Expressive UI, modern elevation cards, responsive typography.
   - **iOS**: Apple Liquid Glass aesthetic, native blur effects, smooth spring transitions.

3. **Localization / Internationalization (English & German)**:
   - **Strict Rule**: No hardcoded static text in UI components.
   - **Android**: Use `stringResource(R.string.<id>)`. Maintain English (`res/values/strings.xml`) and German (`res/values-de/strings.xml`).
   - **iOS**: Use `String(localized: "key")` or `LocalizedStringKey("key")`. Maintain String Catalogs (`Localizable.xcstrings`) or `.strings` files for English (`en`) and German (`de`).

4. **Mandatory UI Previews**:
   - Every component MUST have preview declarations for:
     - **Light Mode**
     - **Dark Mode**
     - **Multiple Screen Sizes / Font Scale Variations**

---

## 🤖 Android (Jetpack Compose) Guidelines

```kotlin
// Example Component File: ChallengeCard.kt
@Composable
fun ChallengeCard(
    challenge: Challenge,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(text = stringResource(R.string.challenge_join_button))
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun ChallengeCardPreview() {
    WandrTheme {
        ChallengeCard(
            challenge = SampleData.challenge,
            onJoinClick = {}
        )
    }
}
```

---

## 🍏 iOS (SwiftUI) Guidelines

```swift
// Example Component File: ChallengeCardView.swift
import SwiftUI

struct ChallengeCardView: View {
    let challenge: Challenge
    let onJoin: () -> Void

    var body: some View {
        Button(action: onJoin) {
            Text("challenge_join_button", comment: "Button label to join challenge")
        }
    }
}

#Preview("Light Mode") {
    ChallengeCardView(challenge: .sample, onJoin: {})
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ChallengeCardView(challenge: .sample, onJoin: {})
        .preferredColorScheme(.dark)
}
```

---

## 🎨 Theme & Colors (Light / Dark)

Brand: **gold `#FFD700`** (from the legacy Flutter app: launcher icon background, logo), black ink, black dark-mode background. Never hard-code `blue` / `purple` / raw hex in components; use theme tokens.

- **Fills** (buttons, banners, selected states): primary gold with **black** content on top.
  - Android: `MaterialTheme.colorScheme.primary` / `onPrimary`
  - iOS: `Color.wandrPrimary` / `Color.wandrOnPrimary`
- **Coloured text, links, thin icons**: gold on white is unreadable, so use the text-safe accent (deep gold in light mode, gold in dark mode).
  - Android: `MaterialTheme.colorScheme.tertiary` (NOT `primary`)
  - iOS: `Color.wandrAccentText` (= `Color.accentColor`)
- **Secondary accent** (legacy blue in light, soft yellow in dark): Android `colorScheme.secondary`, iOS `Color.wandrSecondary`.
- Wrap Android screens and previews in `WandrTheme { }` (`ui/theme/Theme.kt`), not plain `MaterialTheme`. Colors live in `ui/theme/Color.kt` and `Assets.xcassets` (`BrandPrimary`, `OnBrandPrimary`, `BrandSecondary`, `AccentColor`, `LaunchBackground`).
- App icons and splash come from the legacy app (`mipmap-*` on Android, `AppIcon` on iOS); do not regenerate them.

---

## 🧭 Screen Structure (Main Screen & Tabs)

The post-login root is the **Main screen** (`MainScreen` / `MainView`, `MainViewModel`, `MainRoute`): it only owns the bottom tab bar and the tab selection. **Every content screen inside a tab owns its own top app bar**; the main screen does not draw one.

- Android: wrap each screen in `ScreenScaffold(title, onBack?, actions, snackbarHost, floatingActionButton)` (`ui/common/ScreenScaffold.kt`). It applies no window insets to the content, because the main screen's tab bar is outside; pass the returned padding to the content. Drill-down screens (e.g. group details) pass `onBack`.
- iOS: every tab root is its own `NavigationStack` with `.navigationTitle` and `.toolbar` items (edit, sign out, ...). Drill-down uses `navigationDestination` inside that stack; do not hide the navigation bar.
- Screen-level actions (Edit, Sign out) go into the top app bar / toolbar, not into the content body, and the screen title is not repeated in the body.

---

## 💉 Android: Screens Inject Their ViewModels

`WandrApp` only wires routes and navigation callbacks. It never passes view model state down. Each screen takes its view model as a trailing default parameter and collects the state itself:

```kotlin
// ProfileScreen.kt: one file per screen
@Composable
fun ProfileScreen(userId: String, onLogout: () -> Unit, modifier: Modifier = Modifier,
                  viewModel: ProfileViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(userId) { viewModel.processIntent(ProfileIntent.LoadProfile(userId)) }
    ProfileScreenContent(state = state, onIntent = viewModel::processIntent, onLogout = onLogout, modifier = modifier)
}

@Composable
private fun ProfileScreenContent(state: ProfileState, onIntent: (ProfileIntent) -> Unit, onLogout: () -> Unit, modifier: Modifier = Modifier) { … }

@Preview private fun ProfileScreenPreview() { WandrTheme { ProfileScreenContent(state = ProfileState(…), …) } }
```

- The screen file holds the public `XScreen` (view model wiring: ids to load, navigation callbacks, loading intents) and a **private** stateless `XScreenContent` that the previews call (Koin is not available in previews). There are no separate `*Content.kt` files.
- Reusable child components (e.g. `ProfileEditScreen` in the bottom sheet, `TeamCard`) stay in their own files and take plain parameters.
- Screens that must not share state use separate instances (view models are Koin `factory`s); shared data flows through Room.
- Tab screens are composed only while selected, so their view models are recreated on tab switches; keep durable state in the repository / Room cache, not in the view model.

---

## ✏️ Edit Flows (Detail Screens)

Detail screens (profile, team/group, ...) are **read-only**. Editing happens in a **separate edit screen presented in a bottom sheet**, opened by an "Edit" button (shown only when the user may edit).

- Read-only screen: `LabeledValue` (Android) / `LabeledValueView` (iOS) rows and a snackbar host. Avatar and cover are **not** part of the edit sheet: whoever may edit changes them **directly on the detail screen** by tapping the image (`rememberImagePickerFlow` / `.imagePickerFlow`); the edit sheet only holds text, toggles and date fields.
- Edit screen = its own component with Cancel / Save buttons and its own snackbar host (e.g. `ProfileEditScreen`, `TeamEditScreen` / `ProfileEditView`, `TeamEditView`).
- Android: `ModalBottomSheet` with `skipPartiallyExpanded = true`; block swipe-away while saving (`confirmValueChange`); close with `sheetState.hide()` before removing it from composition. iOS: `.sheet` with `.presentationDetents([.large])` and `.interactiveDismissDisabled(isSaving)`.
- Dismissing the sheet (Cancel, swipe, scrim) discards unsaved edits (`DiscardChanges` intent); a successful save closes it and shows a "saved" snackbar on the read-only screen. Errors and image updates show on the sheet.
- Image changes use the reusable crop flow (`rememberImagePickerFlow` / `.imagePickerFlow`) with the specs from `AvatarImageSpec` (1:1, 512 px) and `CoverImageSpec` (4:3, max 1024 px wide); avatars and covers follow the same three layers: `AvatarImage` / `CoverImage` (display only), `AvatarImagePicker` / `CoverImagePicker` (tappable), `AvatarEditor` / `CoverEditor` (picker plus the source -> crop flow).
- The same sheet component serves **creating and editing** (e.g. `TeamEditScreen` with a create or edit title); there is no separate full-screen create form.
- Dates use `DateTimePickerField` (date picker, then time picker); never a free-text duration.

---

## 🔐 Permission Disclosure (Strict Rule)

**Never trigger a system permission prompt cold.** If a permission is not granted yet, first show a *disclosure dialog* that explains **why** WANDR needs it, **what** data is used, and **when** it is collected. Only after the user confirms ("Continue") request the permission from the system.

Flow (both platforms):
1. Permission already granted → run the action directly, no dialog.
2. Not granted / not determined → **disclosure dialog** ("Continue" / "Not Now"). "Not Now" does nothing and keeps the app usable.
3. "Continue" → system permission prompt.
4. Denied and the system will not ask again (Android: no rationale after denial; iOS: `.denied` / `.restricted`) → **denied dialog** with an "Open Settings" shortcut. On iOS check the status *before* asking, so a permanently denied permission skips the disclosure and goes straight to this dialog.

Disclosure text rules:
- Localized in English and German (see localization rule), one `permission_<name>_title` / `_message` / `_denied` string set per permission.
- Concrete and honest: the feature it enables, that data is only collected while the feature is active, and what is *not* done (e.g. nothing saved to the photo library).
- Mention optional permissions that are requested in the same step (e.g. notifications on Android 13+).
- Keep manifest / `Info.plist` usage descriptions in sync (`NSCameraUsageDescription`, `NSLocationWhenInUseUsageDescription`, `<uses-permission>`).

Reusable building blocks (use them, don't re-implement):

| | Android (Compose) | iOS (SwiftUI) |
|---|---|---|
| Disclosure dialog | `ui/permission/PermissionDisclosureDialog.kt` | `.permissionDisclosure(isPresented:title:message:onContinue:)` in `UI/Common/PermissionDisclosureAlert.swift` |
| Denied → Settings | `ui/permission/PermissionDeniedDialog.kt` | `.permissionDeniedAlert(isPresented:message:)` in `UI/Common/PermissionDeniedAlert.swift` |
| Orchestration | `rememberPermissionGate(required, optional, disclosureTitle, disclosureMessage, deniedMessage)`, then `gate.request { action }` in `ui/permission/PermissionGate.kt` | check status (`CameraPermission.status`, `LocationTrackingManager.authorizationStatus`), then show disclosure / denied alert / run action |

Android example:

```kotlin
val locationPermission = rememberPermissionGate(
    required = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
    optional = listOf(Manifest.permission.ACCESS_COARSE_LOCATION),
    disclosureTitle = R.string.permission_location_title,
    disclosureMessage = R.string.permission_location_message,
    deniedMessage = R.string.permission_location_denied
)
Button(onClick = { locationPermission.request { onStartTracking() } }) { ... }
```

iOS example:

```swift
switch CameraPermission.status {
case .authorized: showCamera = true
case .notDetermined: showCameraDisclosure = true   // .permissionDisclosure(...) { request, then open camera }
default: showCameraDenied = true                    // .permissionDeniedAlert(...)
}
```

Notes:
- Android pickers that need no permission (Photo Picker, `TakePicture` via the system camera app) need no disclosure. iOS `PhotosPicker` needs none either; `UIImagePickerController(.camera)` does.
- Prefer the narrowest permission (iOS "While Using" over "Always"; Android Photo Picker over storage permissions).

---

## 🛠 Verification Checklist
- [ ] Is the component contained in its own dedicated file?
- [ ] Are all UI strings localized in both English (`en`) and German (`de`)?
- [ ] Are previews provided for both Light and Dark mode?
- [ ] Are colors taken from theme tokens (gold for fills, `tertiary` / `wandrAccentText` for coloured text)?
- [ ] Are state changes handled cleanly via lambdas / intents?
- [ ] Is every system permission request preceded by a localized disclosure dialog (unless already granted), with an "Open Settings" fallback when permanently denied?
