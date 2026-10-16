import SwiftUI
@preconcurrency import shared

/// "Challenges" tab: owns its NavigationStack (list -> details) and the create sheet for managers, connected to the
/// shared ChallengeViewModel. `openChallengeId` lets the caller push a challenge's details, e.g. after a tapped
/// notification; it is reset to nil once handled.
struct ChallengesContainerView: View {
    let userId: String?
    let isManager: Bool
    var openChallengeId: Binding<String?> = .constant(nil)

    @StateObject private var observer = ChallengeObserver()
    @State private var path: [String] = []
    @State private var showCreate = false
    @Namespace private var zoom

    /// Drafts are for their creating manager only (the server filters too; the local cache may be older).
    private var visibleChallenges: [Challenge] {
        guard let userId else { return [] }
        return observer.challenges.filter { $0.isVisibleTo(userId: userId, isManager: isManager) }
    }

    var body: some View {
        NavigationStack(path: $path) {
            ChallengeListView(
                challenges: visibleChallenges,
                statuses: observer.statuses,
                socialCounts: observer.socialCounts,
                isLoading: observer.isLoading || userId == nil,
                zoomNamespace: zoom,
                onOpen: { path.append($0.id) },
                onToggleLike: { observer.send(ChallengeIntentToggleChallengeLike(challengeId: $0.id)) },
                onRefresh: { await observer.refresh() }
            )
            .navigationTitle(LocalizedStringKey("tab_challenges"))
            .toolbar {
                if isManager {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            observer.send(ChallengeIntentStartCreate.shared)
                            showCreate = true
                        } label: {
                            Label(LocalizedStringKey("challenge_new_button"), systemImage: "plus")
                        }
                        .disabled(userId == nil)
                    }
                }
            }
            .navigationDestination(for: String.self) { challengeId in
                if let userId {
                    ChallengeDetailView(challengeId: challengeId, userId: userId)
                        .challengeZoomDestination(id: challengeId, in: zoom)
                }
            }
            .snackbar($observer.snackbar)
        }
        .sheet(isPresented: $showCreate, onDismiss: { observer.send(ChallengeIntentDiscardForm.shared) }) {
            if let form = observer.form, let userId {
                ChallengeEditView(
                    form: form,
                    isSaving: observer.isSaving,
                    onIntent: { observer.send($0) },
                    onSave: { observer.send(ChallengeIntentSubmitForm(userId: userId)) },
                    onCancel: { showCreate = false },
                    snackbar: $observer.snackbar
                )
                .interactiveDismissDisabled(observer.isSaving)
            } else {
                ProgressView()
            }
        }
        .onChange(of: observer.saveCompletedCount) { _, _ in showCreate = false }
        .task(id: userId) {
            if let userId { observer.load(userId: userId) }
        }
        .onChange(of: openChallengeId.wrappedValue, initial: true) { _, challengeId in
            guard let challengeId else { return }
            // Replace whatever is open, so "back" leads to the list.
            path = [challengeId]
            openChallengeId.wrappedValue = nil
        }
    }
}

// The live container needs Koin and the network; previews show the list it hosts.
#Preview("Light Mode") {
    NavigationStack {
        ChallengeListView(challenges: [ChallengePreviewData.challenge()], statuses: ["c1": .active]).navigationTitle("Challenges")
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NavigationStack {
        ChallengeListView(challenges: [ChallengePreviewData.challenge(isActive: false)], statuses: ["c1": .draft]).navigationTitle("Challenges")
    }
    .preferredColorScheme(.dark)
}

#Preview("iPhone SE", traits: .fixedLayout(width: 320, height: 568)) {
    NavigationStack { ChallengeListView(isLoading: true).navigationTitle("Challenges") }
}
