import SwiftUI
@preconcurrency import shared

struct MainView: View {
    var onLoggedOut: () -> Void = {}
    @StateObject private var viewModel = MainObserver()
    @StateObject private var watchImport = WatchImportObserver()
    @StateObject private var healthImport = HealthKitImporter()
    @StateObject private var notifications = NotificationsObserver()
    @StateObject private var pushNavigation = PushNavigation.shared
    @State private var selectedTab = 0
    @State private var selectedActivity: Activity?
    @State private var showNotifications = false

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                ActivityHistoryView(activities: viewModel.activities, onSelectActivity: { selectedActivity = $0 })
                    .padding()
                    .navigationTitle(LocalizedStringKey("activities_title"))
                    .navigationDestination(item: $selectedActivity) { activity in
                        ActivityDetailView(activity: activity, userId: viewModel.userId)
                    }
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) {
                            Button {
                                notifications.load()
                                showNotifications = true
                            } label: {
                                Image(systemName: notifications.unreadCount > 0 ? "bell.badge.fill" : "bell")
                                    .overlay(alignment: .topTrailing) {
                                        if notifications.unreadCount > 0 {
                                            Text("\(min(notifications.unreadCount, 99))")
                                                .font(.system(size: 10, weight: .bold))
                                                .foregroundColor(.wandrOnPrimary)
                                                .padding(.horizontal, 4)
                                                .background(Color.wandrPrimary, in: Capsule())
                                                .offset(x: 10, y: -8)
                                        }
                                    }
                            }
                            .accessibilityLabel(Text(LocalizedStringKey("notifications_title")))
                        }
                        ToolbarItem(placement: .primaryAction) {
                            Button(LocalizedStringKey("health_import_button")) { healthImport.start() }
                                .disabled(healthImport.isImporting)
                        }
                    }
                    .permissionDisclosure(
                        isPresented: $healthImport.showDisclosure,
                        title: "healthkit_disclosure_title",
                        message: "healthkit_disclosure_message"
                    ) { healthImport.disclosureConfirmed() }
                    .alert(
                        healthImport.message.map { String(localized: $0) } ?? "",
                        isPresented: Binding(get: { healthImport.message != nil }, set: { if !$0 { healthImport.message = nil } })
                    ) { Button("OK", role: .cancel) {} }
            }
            .tabItem { Label(LocalizedStringKey("tab_activity"), systemImage: "figure.hiking") }
            .tag(0)

            NavigationStack {
                ChallengeListView()
                    .padding()
                    .navigationTitle(LocalizedStringKey("challenges_title"))
            }
            .tabItem { Label(LocalizedStringKey("tab_challenges"), systemImage: "trophy") }
            .tag(1)

            if viewModel.isManager {
                // Owns its NavigationStack (list -> details).
                GroupsContainerView(userId: viewModel.userId)
                    .tabItem { Label(LocalizedStringKey("tab_groups"), systemImage: "person.3") }
                    .tag(2)
            }

            NavigationStack {
                ProfileContainerView(userId: viewModel.userId, onLogout: { viewModel.logout(onLoggedOut) })
            }
            .tabItem { Label(LocalizedStringKey("tab_profile"), systemImage: "person.crop.circle") }
            .tag(3)
        }
        .pushPermissionPrompt(isEnabled: viewModel.userId != nil)
        .task { viewModel.start() }
        .task(id: viewModel.userId) {
            if let userId = viewModel.userId { notifications.start(userId: userId) }
        }
        .onChange(of: pushNavigation.target) { _, target in openPushTarget(target) }
        .onChange(of: viewModel.activities.count) { _, _ in openPushTarget(pushNavigation.target) }
        .sheet(isPresented: $showNotifications, onDismiss: { notifications.refreshBadge() }) {
            NotificationsView(
                notifications: notifications.notifications,
                isLoading: notifications.isLoading,
                onSelect: { item in
                    notifications.markRead(item.id)
                    showNotifications = false
                    pushNavigation.open(entityType: item.entityType, entityId: item.entityId)
                },
                onMarkAllRead: { notifications.markAllRead() },
                onClose: { showNotifications = false }
            )
        }
        .task(id: viewModel.userId) {
            if let userId = viewModel.userId { watchImport.start(userId: userId, teamId: viewModel.teamId) }
        }
        .sheet(isPresented: Binding(get: { watchImport.conflict != nil }, set: { _ in })) {
            if let conflict = watchImport.conflict {
                ActivityConflictView(conflict: conflict, isSaving: watchImport.isSaving) { watchImport.resolve($0) }
            }
        }
    }
}

extension MainView {
    /// Opens the activity a tapped push / notification points to (once it is loaded).
    fileprivate func openPushTarget(_ target: PushTarget?) {
        guard let target else { return }
        if target.entityType == "challenge" {
            selectedTab = 1
            pushNavigation.consume()
        } else if let activity = viewModel.activities.first(where: { $0.id == target.entityId }) {
            selectedTab = 0
            selectedActivity = activity
            pushNavigation.consume()
        }
    }
}

@MainActor
final class MainObserver: ObservableObject {
    @Published var activities: [Activity] = []
    @Published var userId: String?
    @Published var teamId: String?
    @Published var isManager = false

    private let mainViewModel = IosDependencies.shared.mainViewModel()
    private let activity = IosDependencies.shared.activityViewModel()
    nonisolated(unsafe) private var jobs: [any Kotlinx_coroutines_coreJob] = []

    func start() {
        jobs.append(FlowObserverKt.watch(mainViewModel.uiState) { [weak self] value in
            guard let state = value as? MainState else { return }
            Task { @MainActor in
                self?.isManager = state.isManager
                self?.teamId = state.teamId
            }
            guard let userId = state.userId else { return }
            Task { @MainActor in
                self?.userId = userId
                self?.activity.processIntent(intent: ActivityIntentLoadUserActivities(userId: userId))
            }
        })
        jobs.append(FlowObserverKt.watch(activity.state) { [weak self] value in
            guard let state = value as? ActivityState else { return }
            Task { @MainActor in self?.activities = state.activities }
        })
        mainViewModel.load()
    }

    func logout(_ done: @escaping () -> Void) {
        mainViewModel.logout { Task { @MainActor in done() } }
    }

    deinit { jobs.forEach { $0.cancel(cause: nil) } }
}

#Preview("Main") { MainView() }
#Preview("Main Dark") { MainView().preferredColorScheme(.dark) }
