import SwiftUI
@preconcurrency import shared

struct MainView: View {
    var onLoggedOut: () -> Void = {}
    @StateObject private var viewModel = MainObserver()
    @StateObject private var watchImport = WatchImportObserver()
    @StateObject private var healthImport = HealthKitImporter()

    var body: some View {
        TabView {
            NavigationStack {
                ActivityHistoryView(activities: viewModel.activities)
                    .padding()
                    .navigationTitle(LocalizedStringKey("activities_title"))
                    .toolbar {
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

            NavigationStack {
                ChallengeListView()
                    .padding()
                    .navigationTitle(LocalizedStringKey("challenges_title"))
            }
            .tabItem { Label(LocalizedStringKey("tab_challenges"), systemImage: "trophy") }

            if viewModel.isManager {
                // Owns its NavigationStack (list -> details).
                GroupsContainerView(userId: viewModel.userId)
                    .tabItem { Label(LocalizedStringKey("tab_groups"), systemImage: "person.3") }
            }

            NavigationStack {
                ProfileContainerView(userId: viewModel.userId, onLogout: { viewModel.logout(onLoggedOut) })
            }
            .tabItem { Label(LocalizedStringKey("tab_profile"), systemImage: "person.crop.circle") }
        }
        .task { viewModel.start() }
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
