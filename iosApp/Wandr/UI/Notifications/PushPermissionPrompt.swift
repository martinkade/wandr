import SwiftUI
import UIKit
import UserNotifications

/// Asks once (after the disclosure) for the notification permission and registers with APNs.
@MainActor
final class PushPermissionObserver: ObservableObject {
    @Published var showDisclosure = false
    private static let askedKey = "push_permission_asked"

    func check() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let status = settings.authorizationStatus
            Task { @MainActor in
                switch status {
                case .authorized, .provisional, .ephemeral:
                    UIApplication.shared.registerForRemoteNotifications()
                case .notDetermined:
                    if !UserDefaults.standard.bool(forKey: Self.askedKey) {
                        UserDefaults.standard.set(true, forKey: Self.askedKey)
                        self.showDisclosure = true
                    }
                default:
                    break   // denied: never nag, the bell still works
                }
            }
        }
    }

    func disclosureConfirmed() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            guard granted else { return }
            Task { @MainActor in UIApplication.shared.registerForRemoteNotifications() }
        }
    }
}

private struct PushPermissionPrompt: ViewModifier {
    let isEnabled: Bool
    @StateObject private var observer = PushPermissionObserver()

    func body(content: Content) -> some View {
        content
            .task(id: isEnabled) { if isEnabled { observer.check() } }
            .permissionDisclosure(
                isPresented: $observer.showDisclosure,
                title: "permission_notifications_title",
                message: "permission_notifications_message"
            ) { observer.disclosureConfirmed() }
    }
}

extension View {
    /// Shows the notification disclosure once (when `isEnabled`, e.g. signed in), then the system prompt.
    func pushPermissionPrompt(isEnabled: Bool) -> some View {
        modifier(PushPermissionPrompt(isEnabled: isEnabled))
    }
}

#Preview("Light Mode") {
    Color.clear.pushPermissionPrompt(isEnabled: false).preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    Color.clear.pushPermissionPrompt(isEnabled: false).preferredColorScheme(.dark)
}
