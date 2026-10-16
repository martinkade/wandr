import SwiftUI
import UserNotifications

/// Settings of the app on this device, opened from the profile. Push notifications can be switched off here.
struct SettingsView: View {
    @StateObject private var observer = SettingsObserver()
    @Environment(\.scenePhase) private var scenePhase
    /// Whether iOS lets the app show notifications at all (the user may have blocked them in the system settings).
    @State private var systemAllows = true
    @State private var showPushDisclosure = false

    var body: some View {
        SettingsContent(
            pushEnabled: Binding(get: { observer.pushEnabled }, set: { setPushEnabled($0) }),
            isUpdatingPush: observer.isUpdatingPush,
            systemAllowsNotifications: systemAllows,
            appVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "",
            onOpenSystemSettings: {
                if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
            }
        )
        .navigationTitle(LocalizedStringKey("settings_title"))
        .task { observer.start() }
        .permissionDisclosure(
            isPresented: $showPushDisclosure,
            title: "permission_notifications_title",
            message: "permission_notifications_message"
        ) {
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
                Task { @MainActor in
                    if granted { UIApplication.shared.registerForRemoteNotifications() }
                    observer.setPushEnabled(true)
                    await refreshSystemPermission()
                }
            }
        }
        .task(id: scenePhase) { await refreshSystemPermission() }
        .alert(
            LocalizedStringKey("settings_title"),
            isPresented: Binding(get: { observer.errorMessage != nil }, set: { if !$0 { observer.clearError() } })
        ) {
            Button("OK", role: .cancel) { observer.clearError() }
        } message: {
            Text(observer.errorMessage ?? "")
        }
    }

    /// Switching on asks iOS for the permission first when it was never asked (with the disclosure before it).
    private func setPushEnabled(_ enabled: Bool) {
        guard enabled else { observer.setPushEnabled(false); return }
        Task {
            let status = await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
            if status == .notDetermined {
                showPushDisclosure = true
            } else {
                if status == .authorized || status == .provisional { UIApplication.shared.registerForRemoteNotifications() }
                observer.setPushEnabled(true)
            }
        }
    }

    private func refreshSystemPermission() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        systemAllows = settings.authorizationStatus != .denied
    }
}

/// The stateless content, so it has previews.
struct SettingsContent: View {
    @Binding var pushEnabled: Bool
    let isUpdatingPush: Bool
    let systemAllowsNotifications: Bool
    let appVersion: String
    var onOpenSystemSettings: () -> Void = {}

    var body: some View {
        Form {
            Section {
                Toggle(isOn: $pushEnabled) {
                    Label {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(LocalizedStringKey("settings_push_title"))
                            Text(LocalizedStringKey("settings_push_description"))
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    } icon: {
                        Image(systemName: "bell.badge.fill").foregroundStyle(Color.wandrAccentText)
                    }
                }
                .disabled(isUpdatingPush)
                .tint(Color.wandrPrimary)

                // On in the app, but blocked in the system settings: nothing would show.
                if pushEnabled && !systemAllowsNotifications {
                    HStack {
                        Text(LocalizedStringKey("settings_push_blocked"))
                            .font(.footnote)
                            .foregroundStyle(.red)
                        Spacer()
                        Button(LocalizedStringKey("settings_open_system"), action: onOpenSystemSettings)
                            .wandrGlassButton()
                    }
                }
            } header: {
                Text(LocalizedStringKey("settings_notifications_section"))
            } footer: {
                if isUpdatingPush { ProgressView() }
            }

            Section(LocalizedStringKey("settings_about_section")) {
                LabeledContent(LocalizedStringKey("settings_version"), value: appVersion)
            }
        }
    }
}

#Preview("Light Mode") {
    NavigationStack {
        SettingsContent(pushEnabled: .constant(true), isUpdatingPush: false, systemAllowsNotifications: true, appVersion: "1.0.0")
            .navigationTitle("Settings")
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, blocked") {
    NavigationStack {
        SettingsContent(pushEnabled: .constant(true), isUpdatingPush: false, systemAllowsNotifications: false, appVersion: "1.0.0")
            .navigationTitle("Settings")
    }
    .preferredColorScheme(.dark)
}
