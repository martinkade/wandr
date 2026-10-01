---
name: wandr-ui-components
description: >-
  Provides standards and workflows for building UI components in SwiftUI (iOS)
  and Jetpack Compose (Android), including preview annotations, dark mode support,
  internationalization/localization (English & German), and modular file layout.
---

# WANDR UI Components Skill

Use this skill whenever creating, editing, or styling UI components for Android (Jetpack Compose) or iOS (SwiftUI).

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

## 🛠 Verification Checklist
- [ ] Is the component contained in its own dedicated file?
- [ ] Are all UI strings localized in both English (`en`) and German (`de`)?
- [ ] Are previews provided for both Light and Dark mode?
- [ ] Are state changes handled cleanly via lambdas / intents?
