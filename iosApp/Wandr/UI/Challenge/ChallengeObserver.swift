import SwiftUI
@preconcurrency import shared

/// Bridges the shared ChallengeViewModel into SwiftUI. Each screen gets its own instance (the view model is a Koin
/// factory); list and details stay in sync through the local database the view model observes.
@MainActor
final class ChallengeObserver: ObservableObject {
    @Published private(set) var challenges: [Challenge] = []
    @Published private(set) var statuses: [String: ChallengeStatus] = [:]
    @Published private(set) var participations: [String: ChallengeParticipation] = [:]
    @Published private(set) var adminTeams: [Team] = []
    @Published private(set) var primaryTeamId: String?
    @Published private(set) var socialCounts: [String: SocialCounts] = [:]
    @Published private(set) var selectedChallenge: Challenge?
    @Published private(set) var canEdit = false
    @Published private(set) var standings: [TeamStanding] = []
    @Published private(set) var leaderboard: [LeaderboardEntry] = []
    @Published private(set) var form: ChallengeForm?
    @Published private(set) var isLoading = false
    @Published private(set) var isSaving = false
    @Published private(set) var isImageUpdating = false
    @Published var snackbar: SnackbarMessage?
    /// Increments after each successful create / update, so the form sheet can close.
    @Published private(set) var saveCompletedCount = 0

    private let viewModel = IosDependencies.shared.challengeViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?
    private var userId: String?

    func load(userId: String) {
        if job == nil {
            job = FlowObserverKt.watch(viewModel.uiState) { [weak self] value in
                guard let state = value as? ChallengeState else { return }
                Task { @MainActor in self?.apply(state) }
            }
        }
        guard self.userId != userId else { return }
        self.userId = userId
        send(ChallengeIntentLoadChallenges(userId: userId))
    }

    /// Pull to refresh: loading again pulls from the server. The view model reports no end of that pull, so the
    /// spinner stays for a short moment only; the list updates whenever the cache changes.
    func refresh() async {
        guard let userId else { return }
        send(ChallengeIntentLoadChallenges(userId: userId))
        try? await Task.sleep(for: .milliseconds(700))
    }

    func select(challengeId: String) { send(ChallengeIntentSelectChallenge(challengeId: challengeId)) }

    func send(_ intent: any ChallengeIntent) { viewModel.processIntent(intent: intent) }

    func uploadCover(_ jpeg: Data) { send(ChallengeIntentUploadCover(jpegBytes: KotlinByteArray.from(data: jpeg))) }

    private func apply(_ state: ChallengeState) {
        challenges = state.challenges
        statuses = state.statuses
        participations = state.participations
        adminTeams = state.adminTeams
        primaryTeamId = state.primaryTeamId
        socialCounts = state.socialCounts
        selectedChallenge = state.selectedChallenge
        canEdit = state.canEdit
        standings = state.standings
        leaderboard = state.leaderboard
        form = state.form
        isLoading = state.isLoading
        isSaving = state.isSaving
        isImageUpdating = state.isImageUpdating

        if let error = state.error {
            snackbar = SnackbarMessage(text: error.userMessage, isError: true)
            send(ChallengeIntentClearMessages.shared)
        } else if let success = state.success {
            if let key = Self.messageKey(success) { snackbar = SnackbarMessage(text: String(localized: key)) }
            if success == .created || success == .updated { saveCompletedCount += 1 }
            send(ChallengeIntentClearMessages.shared)
        }
    }

    private static func messageKey(_ success: ChallengeSuccess) -> String.LocalizationValue? {
        switch success {
        case .created: return "challenge_created_message"
        case .updated: return "challenge_updated_message"
        case .joined: return "challenge_joined_message"
        case .left: return "challenge_left_message"
        case .teamEnrolled: return "challenge_team_enrolled_message"
        case .teamWithdrawn: return "challenge_team_withdrawn_message"
        case .imageUpdated: return "challenge_image_updated_message"
        default: return nil
        }
    }

    deinit { job?.cancel(cause: nil) }
}
