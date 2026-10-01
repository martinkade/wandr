import SwiftUI
@preconcurrency import shared

struct DashboardView: View {
    var onLoggedOut: () -> Void = {}
    @StateObject private var viewModel = DashboardObserver()

    var body: some View {
        TabView {
            ActivityHistoryView(activities: viewModel.activities)
                .padding()
                .tabItem { Label(LocalizedStringKey("tab_activity"), systemImage: "figure.hiking") }

            ChallengeListView()
                .padding()
                .tabItem { Label(LocalizedStringKey("tab_challenges"), systemImage: "trophy") }

            VStack {
                ProfileView()
                Button(role: .destructive) {
                    viewModel.logout(onLoggedOut)
                } label: {
                    Text(LocalizedStringKey("logout_button"))
                }
                .padding(.bottom)
            }
            .tabItem { Label(LocalizedStringKey("tab_profile"), systemImage: "person.crop.circle") }
        }
        .task { viewModel.start() }
    }
}

@MainActor
final class DashboardObserver: ObservableObject {
    @Published var activities: [Activity] = []

    private let dashboard = IosDependencies.shared.dashboardViewModel()
    private let activity = IosDependencies.shared.activityViewModel()
    nonisolated(unsafe) private var jobs: [any Kotlinx_coroutines_coreJob] = []

    func start() {
        jobs.append(FlowObserverKt.watch(dashboard.uiState) { [weak self] value in
            guard let state = value as? DashboardState, let userId = state.userId else { return }
            Task { @MainActor in
                self?.activity.processIntent(intent: ActivityIntentLoadUserActivities(userId: userId))
            }
        })
        jobs.append(FlowObserverKt.watch(activity.state) { [weak self] value in
            guard let state = value as? ActivityState else { return }
            Task { @MainActor in self?.activities = state.activities }
        })
        dashboard.load()
    }

    func logout(_ done: @escaping () -> Void) {
        dashboard.logout { Task { @MainActor in done() } }
    }

    deinit { jobs.forEach { $0.cancel(cause: nil) } }
}

#Preview("Dashboard") { DashboardView() }
#Preview("Dashboard Dark") { DashboardView().preferredColorScheme(.dark) }
