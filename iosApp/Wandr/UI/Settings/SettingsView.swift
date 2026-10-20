import SwiftUI
import UserNotifications
@preconcurrency import shared

/// Settings, opened from the profile: push on this device, what to be notified about, and resetting or deleting the
/// account. After a deletion [onAccountDeleted] takes the user back to the sign-in (this device is signed out already).
struct SettingsView: View {
    let userId: String?
    var onAccountDeleted: () -> Void = {
    }

    @StateObject private var observer = SettingsObserver()
    @Environment(\.scenePhase) private var scenePhase
    /// Whether iOS lets the app show notifications at all (the user may have blocked them in the system settings).
    @State private var systemAllows = true
    @State private var showPushDisclosure = false
    @State private var confirm: AccountAction?
    @State private var showResetDone = false

    var body: some View {
        SettingsContent(
            pushEnabled: Binding(get: { observer.pushEnabled }, set: { setPushEnabled($0) }),
            isUpdatingPush: observer.isUpdatingPush,
            systemAllowsNotifications: systemAllows,
            scopes: observer.preferences.map { prefs in
                Dictionary(uniqueKeysWithValues: NotificationScopeOption.allCases.map {
                    ($0, prefs.isEnabled(scope: $0.scope))
                })
            },
            isResettingAccount: observer.isResettingAccount,
            isDeletingAccount: observer.isDeletingAccount,
            appVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "",
            onScopeChange: { option, enabled in observer.setScope(option.scope, enabled: enabled) },
            onResetAccount: { confirm = .reset },
            onDeleteAccount: { confirm = .delete },
            onOpenSystemSettings: {
                if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
            }
        )
        .navigationTitle(LocalizedStringKey("settings_title"))
        .task(id: userId) {
            if let userId {
                observer.start(userId: userId)
            }
        }
        .task(id: scenePhase) {
            await refreshSystemPermission()
        }
        .onChange(of: observer.success) { _, success in
            guard let success else {
                return
            }
            observer.clearMessages()
            switch success {
            case .accountDeleted: onAccountDeleted()
            case .accountReset: showResetDone = true
            default: break
            }
        }
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
        .confirmationDialog(
            Text(LocalizedStringKey(confirm == .delete ? "settings_account_delete_confirm_title" : "settings_account_reset_confirm_title")),
            isPresented: Binding(get: { confirm != nil }, set: { if !$0 {
                confirm = nil
            } }),
            titleVisibility: .visible
        ) {
            Button(LocalizedStringKey(confirm == .delete ? "settings_account_delete_confirm" : "settings_account_reset_confirm"), role: .destructive) {
                if confirm == .delete {
                    observer.deleteAccount()
                } else {
                    observer.resetAccount()
                }
                confirm = nil
            }
            Button(LocalizedStringKey("cancel_button"), role: .cancel) {
                confirm = nil
            }
        } message: {
            Text(LocalizedStringKey(confirm == .delete ? "settings_account_delete_confirm_message" : "settings_account_reset_confirm_message"))
        }
        .alert(LocalizedStringKey("settings_account_reset_done"), isPresented: $showResetDone) {
            Button("OK", role: .cancel) {
            }
        }
        .alert(
            LocalizedStringKey("settings_title"),
            isPresented: Binding(get: { observer.errorMessage != nil }, set: { if !$0 {
                observer.clearMessages()
            } })
        ) {
            Button("OK", role: .cancel) {
                observer.clearMessages()
            }
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

private enum AccountAction {
    case reset, delete
}

/// The notification choices in the order of the screen, with their labels.
enum NotificationScopeOption: CaseIterable, Hashable {
    case activityLikes, activityComments, challengeComments

    var scope: NotificationScope {
        switch self {
        case .activityLikes: return .activityLikes
        case .activityComments: return .activityComments
        case .challengeComments: return .challengeComments
        }
    }

    var label: LocalizedStringKey {
        switch self {
        case .activityLikes: return "settings_scope_activity_likes"
        case .activityComments: return "settings_scope_activity_comments"
        case .challengeComments: return "settings_scope_challenge_comments"
        }
    }

    var symbol: String {
        switch self {
        case .activityLikes: return "heart"
        case .activityComments: return "bubble.left"
        case .challengeComments: return "trophy"
        }
    }
}

/// The stateless content, so it has previews.
struct SettingsContent: View {
    @Binding var pushEnabled: Bool
    let isUpdatingPush: Bool
    let systemAllowsNotifications: Bool
    /// nil while the choices load from the server.
    let scopes: [NotificationScopeOption: Bool]?
    var isResettingAccount = false
    var isDeletingAccount = false
    let appVersion: String
    var onScopeChange: (NotificationScopeOption, Bool) -> Void = { _, _ in
    }
    var onResetAccount: () -> Void = {
    }
    var onDeleteAccount: () -> Void = {
    }
    var onOpenSystemSettings: () -> Void = {}

    private var accountBusy: Bool {
        isResettingAccount || isDeletingAccount
    }

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

            Section {
                ForEach(NotificationScopeOption.allCases, id: \.self) { option in
                    Toggle(isOn: Binding(get: { scopes?[option] ?? true }, set: { onScopeChange(option, $0) })) {
                        Label(option.label, systemImage: option.symbol)
                    }
                    .tint(Color.wandrPrimary)
                    // Disabled until the choice is loaded from the server.
                    .disabled(scopes == nil)
                }
            } header: {
                Text(LocalizedStringKey("settings_scopes_section"))
            } footer: {
                Text(LocalizedStringKey("settings_scopes_hint"))
            }

            Section(LocalizedStringKey("settings_account_section")) {
                accountButton("settings_account_reset", "settings_account_reset_description", busy: isResettingAccount, action: onResetAccount)
                accountButton("settings_account_delete", "settings_account_delete_description", busy: isDeletingAccount, action: onDeleteAccount)
            }

            Section(LocalizedStringKey("settings_about_section")) {
                LabeledContent(LocalizedStringKey("settings_version"), value: appVersion)
            }
        }
    }

    private func accountButton(_ title: LocalizedStringKey, _ description: LocalizedStringKey, busy: Bool, action: @escaping () -> Void) -> some View {
        Button(role: .destructive, action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                    Text(description).font(.footnote).foregroundStyle(.secondary)
                }
                Spacer()
                if busy {
                    ProgressView()
                }
            }
        }
        .disabled(accountBusy)
    }
}

#Preview("Light Mode") {
    NavigationStack {
        SettingsContent(
            pushEnabled: .constant(true), isUpdatingPush: false, systemAllowsNotifications: true,
            scopes: [.activityLikes: false, .activityComments: true, .challengeComments: true], appVersion: "1.0.0"
        )
            .navigationTitle("Settings")
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, blocked, loading") {
    NavigationStack {
        SettingsContent(
            pushEnabled: .constant(true), isUpdatingPush: false, systemAllowsNotifications: false,
            scopes: nil, isDeletingAccount: true, appVersion: "1.0.0"
        )
            .navigationTitle("Settings")
    }
    .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    NavigationStack {
        SettingsContent(
            pushEnabled: .constant(false), isUpdatingPush: false, systemAllowsNotifications: true,
            scopes: [.activityLikes: true, .activityComments: true, .challengeComments: true], appVersion: "1.0.0"
        )
    }
    .environment(\.dynamicTypeSize, .accessibility2)
}
