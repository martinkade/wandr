import SwiftUI
@preconcurrency import shared

/// Details of one challenge, connected to its own ChallengeViewModel. Works standalone: pushed from the list or
/// opened from a notification about the challenge. The creator can edit it (sheet) and change its cover; team
/// owners / admins enroll or withdraw a team, everybody else joins or leaves (individual challenges).
struct ChallengeDetailView: View {
    let challengeId: String
    let userId: String

    @StateObject private var observer = ChallengeObserver()
    @State private var showEdit = false
    @State private var showEnroll = false
    @State private var showCoverPicker = false

    var body: some View {
        content
            .task(id: challengeId) {
                observer.load(userId: userId) // admin teams, participations and runtime statuses
                observer.select(challengeId: challengeId)
            }
            .snackbar($observer.snackbar)
            .sheet(isPresented: $showEdit, onDismiss: { observer.send(ChallengeIntentDiscardForm.shared) }) {
                editSheet
            }
            .sheet(isPresented: $showEnroll) {
                EnrollTeamSheetView(
                    teams: enrollableTeams,
                    primaryTeamId: observer.primaryTeamId,
                    onSelect: { team in
                        showEnroll = false
                        observer.send(ChallengeIntentEnrollTeam(challengeId: challengeId, teamId: team.id, userId: userId))
                    },
                    onCancel: { showEnroll = false }
                )
                .presentationDetents([.medium, .large])
            }
            .imagePickerFlow(
                isPresented: $showCoverPicker,
                title: "team_cover_title",
                aspectRatio: CoverImageSpec.shared.ASPECT_RATIO,
                maxEdge: Int(CoverImageSpec.shared.MAX_EDGE_PX),
                jpegQuality: Int(CoverImageSpec.shared.JPEG_QUALITY),
                canRemove: observer.selectedChallenge?.coverUrl != nil,
                onImageReady: { observer.uploadCover($0) },
                onRemove: { observer.send(ChallengeIntentRemoveCover.shared) }
            )
            .onChange(of: observer.saveCompletedCount) { _, _ in showEdit = false }
    }

    @ViewBuilder
    private var content: some View {
        if let challenge = observer.selectedChallenge, challenge.id == challengeId {
            let status = observer.statuses[challenge.id] ?? .active
            ChallengeDetailContentView(
                challenge: challenge,
                status: status,
                userId: userId,
                action: action(for: challenge, status: status),
                standings: observer.standings,
                leaderboard: observer.leaderboard,
                primaryTeamId: observer.primaryTeamId,
                canEdit: observer.canEdit,
                isImageUpdating: observer.isImageUpdating,
                onAction: perform,
                onEdit: {
                    observer.send(ChallengeIntentStartEdit(challengeId: challenge.id))
                    showEdit = true
                },
                onChangeCover: { showCoverPicker = true }
            ) {
                // Likes and comments; the creator may also delete other users' comments.
                EntitySocialBar(type: .challenge, entityId: challenge.id, userId: userId, ownerId: challenge.createdBy)
            }
        } else {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.systemGroupedBackground))
        }
    }

    @ViewBuilder
    private var editSheet: some View {
        if let form = observer.form {
            ChallengeEditView(
                form: form,
                isSaving: observer.isSaving,
                onIntent: { observer.send($0) },
                onSave: { observer.send(ChallengeIntentSubmitForm(userId: userId)) },
                onCancel: { showEdit = false },
                snackbar: $observer.snackbar
            )
            .interactiveDismissDisabled(observer.isSaving)
        } else {
            ProgressView()
        }
    }

    private var adminTeamIds: Set<String> { Set(observer.adminTeams.map(\.id)) }

    /// The standings tell which teams are enrolled.
    private var enrolledTeamIds: Set<String> { Set(observer.standings.map(\.teamId)) }

    /// Only administered teams that are not enrolled yet can be chosen.
    private var enrollableTeams: [Team] { observer.adminTeams.filter { !enrolledTeamIds.contains($0.id) } }

    /// Join / leave (individual) or enroll / withdraw a team; none for drafts and finished challenges. For team
    /// challenges it waits for the standings, so the button does not flicker between enroll and withdraw.
    private func action(for challenge: Challenge, status: ChallengeStatus) -> ChallengeAction? {
        if challenge.isTeamChallenge && observer.isLoading { return nil }
        return ChallengeActionKt.availableChallengeAction(
            challenge: challenge,
            status: status,
            participation: observer.participations[challenge.id],
            adminTeamIds: adminTeamIds,
            enrolledTeamIds: enrolledTeamIds
        )
    }

    private func perform(_ action: ChallengeAction) {
        switch action {
        case .join: observer.send(ChallengeIntentJoinChallenge(challengeId: challengeId, userId: userId))
        case .leave: observer.send(ChallengeIntentLeaveChallenge(challengeId: challengeId, userId: userId))
        case .enrollTeam: showEnroll = true
        case .withdrawTeam:
            if let teamId = adminTeamIds.first(where: enrolledTeamIds.contains) {
                observer.send(ChallengeIntentWithdrawTeam(challengeId: challengeId, teamId: teamId))
            }
        default: break
        }
    }
}

// The live view needs Koin and the network, so the previews show the content it hosts.
@MainActor
private func previewContent(_ status: ChallengeStatus, action: ChallengeAction?) -> some View {
    NavigationStack {
        ChallengeDetailContentView(
            challenge: ChallengePreviewData.challenge(scope: "individual"),
            status: status,
            userId: "u1",
            action: action,
            leaderboard: ChallengePreviewData.leaderboard
        ) {
            SocialBarView(likeCount: 4, likedByMe: false, commentCount: 1)
        }
    }
}

#Preview("Light Mode") { previewContent(.active, action: .join).preferredColorScheme(.light) }

#Preview("Dark Mode") { previewContent(.active, action: .leave).preferredColorScheme(.dark) }

#Preview("iPhone SE", traits: .fixedLayout(width: 320, height: 568)) { previewContent(.expired, action: nil) }
